package io.bennyoe.systems.render

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.maps.tiled.TiledMapImageLayer
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer
import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IntervalSystem
import com.github.quillraven.fleks.World.Companion.inject
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

    // Storage for map layers from MapChangedEvent
    private val mapTileLayers = mutableListOf<TiledMapTileLayer>()
    private val mapImageLayers = mutableListOf<TiledMapImageLayer>()

    // The unified render queue - rebuilt each frame
    private val renderQueue = mutableListOf<RenderableElement>()

    private val shaderService = ShaderService()
    private val lightingRenderer = LightingRenderer(stage, lightEngine, mapRenderer, shaderService)

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

        // 1. Update camera and stage
        orthoCam.update()
        stage.viewport.apply()
        stage.act(deltaTime)

        // 3. Build the render queue with everything sorted by zIndex
        buildRenderQueue()

        // 4. start the fbo to draw in multiple passes
        renderTargets.fbo.begin()

        // 5. Update lighting system
        lightEngine.update()

        // 6. Render everything in the correct order
        lightingRenderer.render(renderQueue, playerActor, orthoCam, gameStateCmp, continuousTime, rainMaskFamily)

        renderTargets.fbo.end()

        // 7. render the fbo texture to the screen
        val tex = renderTargets.fbo.colorBufferTexture
        val waterFamily = world.family { all(WaterComponent, TransformComponent) }

        // 8. draw the fbo to the screen
        stage.batch.projectionMatrix = orthoCam.combined
        stage.batch.use {
            it.shader = null // Ensure default shader
            val w = orthoCam.viewportWidth * orthoCam.zoom
            val h = orthoCam.viewportHeight * orthoCam.zoom
            val x = orthoCam.position.x - w * 0.5f
            val y = orthoCam.position.y - h * 0.5f

            it.draw(
                tex,
                x,
                y,
                w,
                h,
                0,
                0,
                tex.width,
                tex.height,
                false,
                true,
            )
        }

        // 9. draw the water effect over the already rendered scene
        stage.batch.begin()
        waterFamily.forEach { e ->
            val water = e[WaterComponent]
            val tr = e[TransformComponent]

            // Activate and configure the water shader
            stage.batch.shader = water.shader
            water.shader?.bind()

            shaderService.updateUniformsPerFrame(
                shader = water.shader!!,
                uniforms = water.uniforms,
                continuousTime = continuousTime,
            )

            val (sx, sy, sw, sh) =
                worldRectToTexRegion(
                    tr.position.x,
                    tr.position.y,
                    tr.width,
                    tr.height,
                    orthoCam,
                    tex.width,
                    tex.height,
                )

            // Draw the specific region of the FBO texture with the water shader
            stage.batch.draw(
                tex,
                tr.position.x,
                tr.position.y,
                tr.width,
                tr.height,
                sx,
                sy,
                sw,
                sh,
                false,
                true,
            )
        }
        stage.batch.shader = null
        stage.batch.end()

        // 10. draw the final box2dLights
        lightEngine.renderBox2dLights()
    }

    private fun buildRenderQueue() {
        renderQueue.clear()

        // 1. Add all map image layers
        mapImageLayers.forEach { layer ->
            if (layer.textureRegion == null) return
            val zIndex = layer.properties.get("zIndex", Int::class.java) ?: 0
            renderQueue.add(RenderableElement.ImageLayer(layer, zIndex))
        }

        // 2. Add all map tile layers
        mapTileLayers.forEach { layer ->
            val zIndex = layer.properties.get("zIndex", Int::class.java) ?: 7000
            renderQueue.add(RenderableElement.TileLayer(layer, zIndex))
        }

        // 3. Add all entities
        world.family { any(ImageComponent, ParticleComponent) }.forEach { entity ->
            val shaderRenderingCmp = entity.getOrNull(ShaderRenderingComponent)
            val particleCmp = entity.getOrNull(ParticleComponent)
            val hitEffectCmp = entity.getOrNull(HitEffectComponent)
            entity.getOrNull(ImageComponent)?.let { imageCmp ->
                renderQueue.add(
                    RenderableElement.EntityWithImage(
                        entity = entity,
                        imageCmp = imageCmp,
                        shaderRenderingCmp = shaderRenderingCmp,
                        particleCmp = particleCmp,
                        hitEffectComponent = hitEffectCmp,
                        zIndex = imageCmp.zIndex,
                    ),
                )
            }

            entity.getOrNull(ParticleComponent)?.let { particleCmp ->
                // Only add particle if entity doesn't have image (to avoid duplicates)
                if (!entity.has(ImageComponent)) {
                    renderQueue.add(
                        RenderableElement.EntityWithParticle(
                            entity = entity,
                            particleCmp = particleCmp,
                            zIndex = particleCmp.zIndex,
                        ),
                    )
                }
            }
        }

        // 4. Sort everything by zIndex
        renderQueue.sortBy { it.zIndex }
    }

    private fun worldRectToTexRegion(
        rectX: Float,
        rectY: Float,
        rectW: Float,
        rectH: Float,
        cam: OrthographicCamera,
        texW: Int,
        texH: Int,
    ): IntArray {
        // visible world-area of FBO to world coordinates
        val viewW = cam.viewportWidth * cam.zoom
        val viewH = cam.viewportHeight * cam.zoom
        val viewX = cam.position.x - viewW * 0.5f
        val viewY = cam.position.y - viewH * 0.5f

        // normalized u,v in [0..1] relative to FBO
        val u0 = ((rectX - viewX) / viewW).coerceIn(0f, 1f)
        val v0 = ((rectY - viewY) / viewH).coerceIn(0f, 1f)
        val u1 = (((rectX + rectW) - viewX) / viewW).coerceIn(0f, 1f)
        val v1 = (((rectY + rectH) - viewY) / viewH).coerceIn(0f, 1f)

        // convert to texel
        val sx = (u0 * texW).toInt()
        val sy = (v0 * texH).toInt()
        val sw = ((u1 - u0) * texW).toInt().coerceAtLeast(1)
        val sh = ((v1 - v0) * texH).toInt().coerceAtLeast(1)

        return intArrayOf(sx, sy, sw, sh)
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
        val shaderRenderingCmp: ShaderRenderingComponent?,
        val particleCmp: ParticleComponent?,
        val hitEffectComponent: HitEffectComponent?,
        override val zIndex: Int,
    ) : RenderableElement()

    data class EntityWithParticle(
        val entity: Entity,
        val particleCmp: ParticleComponent,
        override val zIndex: Int,
    ) : RenderableElement()
}
