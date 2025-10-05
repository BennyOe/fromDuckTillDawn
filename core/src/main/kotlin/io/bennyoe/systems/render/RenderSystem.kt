package io.bennyoe.systems.render

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.maps.tiled.TiledMapImageLayer
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer
import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IntervalSystem
import com.github.quillraven.fleks.World.Companion.inject
import io.bennyoe.components.ChainRenderComponent
import io.bennyoe.components.GameStateComponent
import io.bennyoe.components.HitEffectComponent
import io.bennyoe.components.ImageComponent
import io.bennyoe.components.ParticleComponent
import io.bennyoe.components.PlayerComponent
import io.bennyoe.components.RainMaskComponent
import io.bennyoe.components.ShaderRenderingComponent
import io.bennyoe.components.TransformComponent
import io.bennyoe.components.WaterComponent
import io.bennyoe.config.GameConstants.SHOW_ONLY_DEBUG
import io.bennyoe.config.GameConstants.UNIT_SCALE
import io.bennyoe.event.MapChangedEvent
import io.bennyoe.lightEngine.core.Scene2dLightEngine
import io.bennyoe.screens.RenderTargets
import io.bennyoe.utility.findImageLayerDeep
import io.bennyoe.utility.findTileLayerDeep
import ktx.graphics.use
import ktx.log.logger

class RenderSystem(
    private val stage: Stage = inject("stage"),
    private val lightEngine: Scene2dLightEngine = inject("lightEngine"),
    private val renderTargets: RenderTargets = inject("renderTargets"),
    polygonSpriteBatch: PolygonSpriteBatch = inject("polygonSpriteBatch"),
    waterAtlas: TextureAtlas = inject("waterAtlas"),
) : IntervalSystem(
        enabled = !SHOW_ONLY_DEBUG,
    ),
    EventListener {
    private var continuousTime = 0f
    private val mapRenderer = OrthogonalTiledMapRenderer(null, UNIT_SCALE, stage.batch)
    private val orthoCam = stage.camera as OrthographicCamera
    private val gameStateCmp by lazy { world.family { all(GameStateComponent) }.first()[GameStateComponent] }
    private val playerActor by lazy { world.family { all(PlayerComponent) }.first()[ImageComponent].image }
    private val rainMaskFamily by lazy { world.family { all(RainMaskComponent) } }
    private val chainFamily by lazy { world.family { all(ChainRenderComponent) } }

    // Storage for map layers from MapChangedEvent
    private val mapTileLayers = mutableListOf<TiledMapTileLayer>()
    private val mapImageLayers = mutableListOf<TiledMapImageLayer>()

    // The unified render queue - rebuilt each frame
    private val renderQueue = mutableListOf<RenderableElement>()

    // The unified render queue that gets rendered on top of the water - rebuilt each frame
    private val renderQueueOnTopOfWater = mutableListOf<RenderableElement>()

    private val shaderService = ShaderService()
    private val waterRenderer = WaterRenderer(stage, polygonSpriteBatch, waterAtlas)
    private val lightingRenderer = LightingRenderer(stage, world, lightEngine, mapRenderer, shaderService)

    override fun handle(event: Event?): Boolean {
        when (event) {
            is MapChangedEvent -> {
                // Clear and store map layers with their zIndex
                mapTileLayers.clear()
                mapImageLayers.clear()
                mapRenderer.map = event.map

                event.map.layers.findTileLayerDeep().forEach { layer ->
                    mapTileLayers.add(layer)
                }
                event.map.layers.findImageLayerDeep().forEach { layer ->
                    mapImageLayers.add(layer)
                }

                return true
            }
        }
        return false
    }

    override fun onTick() {
        continuousTime += deltaTime

        // --- 1. PREPARATION ---
        orthoCam.update()
        stage.viewport.apply()
        stage.act(deltaTime)
        buildRenderQueue()
        lightEngine.update()

        // --- 2. SCENE PASS (into sceneFbo) ---
        renderTargets.fbo.use {
            lightingRenderer.render(renderQueue, playerActor, orthoCam, gameStateCmp, continuousTime, rainMaskFamily, chainFamily)
        }

        // --- 3. RENDER TO SCREEN (render the fbo texture to the screen) ---
        val tex = renderTargets.fbo.colorBufferTexture
        stage.batch.use {
            stage.batch.setBlendFunction(GL20.GL_ONE, GL20.GL_ONE_MINUS_SRC_ALPHA)

            val w = orthoCam.viewportWidth * orthoCam.zoom
            val h = orthoCam.viewportHeight * orthoCam.zoom
            val x = orthoCam.position.x - w * 0.5f
            val y = orthoCam.position.y - h * 0.5f
            stage.batch.draw(tex, x, y, w, h, 0, 0, tex.width, tex.height, false, true)
            stage.batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        }

        // --- 4. RENDER WATER SHADER (on top of the final image) ---
        val waterFamily = world.family { all(WaterComponent, TransformComponent) }
        waterRenderer.render(waterFamily, continuousTime, tex, orthoCam)

        // --- 5. RENDER FOREGROUND TILES (on top of the water) ---
        renderForegroundPlain(stage.batch, orthoCam, renderQueueOnTopOfWater, mapRenderer)

        // --- 6. RENDER LIGHTS (draw the final box2dLights) ---
        lightEngine.renderBox2dLights()
    }

    private fun buildRenderQueue() {
        renderQueue.clear()
        renderQueueOnTopOfWater.clear()

        // 1. Add all map image layers
        mapImageLayers.forEach { layer ->
            if (layer.textureRegion == null) return
            val zIndex = layer.properties.get("zIndex", Int::class.java) ?: 0
            val renderedOnTopOfWater = layer.properties.get("renderedOnTopOfWater", Boolean::class.java) ?: false

            if (renderedOnTopOfWater) {
                renderQueueOnTopOfWater.add(RenderableElement.ImageLayer(layer, zIndex))
            } else {
                renderQueue.add(RenderableElement.ImageLayer(layer, zIndex))
            }
        }

        // 2. Add all map tile layers
        mapTileLayers.forEach { layer ->
            val zIndex = layer.properties.get("zIndex", Int::class.java) ?: 7000
            val renderedOnTopOfWater = layer.properties.get("renderedOnTopOfWater", Boolean::class.java) ?: false

            if (renderedOnTopOfWater) {
                renderQueueOnTopOfWater.add(RenderableElement.TileLayer(layer, zIndex))
            } else {
                renderQueue.add(RenderableElement.TileLayer(layer, zIndex))
            }
        }

        // 3. Add all entities
        world.family { any(ImageComponent, ParticleComponent) }.forEach { entity ->
            val shaderRenderingCmp = entity.getOrNull(ShaderRenderingComponent)
            val hitEffectCmp = entity.getOrNull(HitEffectComponent)
            val transformCmp = entity[TransformComponent]
            entity.getOrNull(ImageComponent)?.let { imageCmp ->
                renderQueue.add(
                    RenderableElement.EntityWithImage(
                        entity = entity,
                        imageCmp = imageCmp,
                        transformCmp = transformCmp,
                        shaderRenderingCmp = shaderRenderingCmp,
                        hitEffectComponent = hitEffectCmp,
                        zIndex = imageCmp.zIndex,
                    ),
                )
            }

            entity.getOrNull(ParticleComponent)?.let { particleCmp ->
                renderQueue.add(
                    RenderableElement.EntityWithParticle(
                        entity = entity,
                        particleCmp = particleCmp,
                        transformCmp = transformCmp,
                        zIndex = particleCmp.zIndex,
                    ),
                )
            }
        }

        // 4. Sort everything by zIndex
        renderQueue.sortBy { it.zIndex }
    }

    /**
     * Renders foreground elements (tile layers, image layers, entities) using the provided batch and camera.
     *
     * This function draws all elements in the given list on top of the water layer, using the default shader.
     * It handles tile layers, image layers, entities with images, and entities with particles.
     *
     * @param batch The batch used for rendering.
     * @param orthoCam The camera providing the projection matrix.
     * @param elements The list of renderable elements to draw.
     * @param mapRenderer The tiled map renderer for tile and image layers.
     */
    private fun renderForegroundPlain(
        batch: Batch,
        orthoCam: OrthographicCamera,
        elements: List<RenderableElement>,
        mapRenderer: OrthogonalTiledMapRenderer,
    ) {
        mapRenderer.setView(orthoCam)
        batch.projectionMatrix = orthoCam.combined

        batch.begin()

        var currentTileset: Texture? = null
        elements.forEach { e ->
            when (e) {
                is RenderableElement.TileLayer -> {
                    mapRenderer.batch.color = Color.WHITE
                    mapRenderer.renderTileLayer(e.layer)
                }

                is RenderableElement.ImageLayer -> {
                    mapRenderer.batch.color = Color.WHITE
                    mapRenderer.renderImageLayer(e.layer)
                }

                is RenderableElement.EntityWithImage -> {
                    val img = e.imageCmp.image
                    val drw = img.drawable as? TextureRegionDrawable ?: return@forEach
                    val region = drw.region

                    val tex = region.texture
                    if (currentTileset !== tex) {
                        currentTileset = tex
                    }

                    batch.draw(
                        region,
                        img.x,
                        img.y,
                        img.originX,
                        img.originY,
                        img.width,
                        img.height,
                        img.scaleX,
                        img.scaleY,
                        img.rotation,
                    )
                }

                is RenderableElement.EntityWithParticle -> {
                    if (e.particleCmp.enabled) {
                        e.particleCmp.actor.setPosition(e.transformCmp.position.x, e.transformCmp.position.y)
                        e.particleCmp.actor.draw(batch, 1f)
                    }
                }
            }
        }

        batch.end()
    }

    companion object {
        val logger = logger<RenderSystem>()
    }
}

enum class ShaderType {
    NONE, // Initial state
    DEFAULT, // Default/basic shader
    LIGHTING, // Engine lighting shader
    CUSTOM, // Custom shader for the sprite (no lighting from the engine is applied)
}

sealed class RenderableElement {
    abstract val zIndex: Int

    data class TileLayer(
        val layer: TiledMapTileLayer,
        override val zIndex: Int,
    ) : RenderableElement()

    data class ImageLayer(
        val layer: TiledMapImageLayer,
        override val zIndex: Int,
    ) : RenderableElement()

    data class EntityWithImage(
        val entity: Entity,
        val imageCmp: ImageComponent,
        val transformCmp: TransformComponent,
        val shaderRenderingCmp: ShaderRenderingComponent?,
        val hitEffectComponent: HitEffectComponent?,
        override val zIndex: Int,
    ) : RenderableElement()

    data class EntityWithParticle(
        val entity: Entity,
        val particleCmp: ParticleComponent,
        val transformCmp: TransformComponent,
        override val zIndex: Int,
    ) : RenderableElement()
}
