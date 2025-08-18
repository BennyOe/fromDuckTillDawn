package io.bennyoe.systems

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.maps.tiled.TiledMapImageLayer
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer
import com.badlogic.gdx.maps.tiled.tiles.AnimatedTiledMapTile
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import com.github.quillraven.fleks.World.Companion.inject
import io.bennyoe.components.GameStateComponent
import io.bennyoe.components.ImageComponent
import io.bennyoe.components.ParticleComponent
import io.bennyoe.components.PhysicComponent
import io.bennyoe.components.PlayerComponent
import io.bennyoe.components.ShaderRenderingComponent
import io.bennyoe.components.TransformComponent
import io.bennyoe.config.GameConstants.SHOW_ONLY_DEBUG
import io.bennyoe.config.GameConstants.UNIT_SCALE
import io.bennyoe.event.MapChangedEvent
import io.bennyoe.lightEngine.core.Scene2dLightEngine
import ktx.graphics.use
import ktx.log.logger
import ktx.tiled.forEachLayer

class RenderSystem(
    private val stage: Stage = inject("stage"),
    private val lightEngine: Scene2dLightEngine = inject("lightEngine"),
) : IteratingSystem(family { all(TransformComponent).any(ImageComponent, ParticleComponent) }, enabled = !SHOW_ONLY_DEBUG),
    EventListener {
    private val mapRenderer = OrthogonalTiledMapRenderer(null, UNIT_SCALE, stage.batch)
    private val mapTileLayer: MutableList<TiledMapTileLayer> = mutableListOf()
    private val mapBg: MutableList<TiledMapImageLayer> = mutableListOf()
    private val orthoCam = stage.camera as OrthographicCamera
    private val gameStateEntity by lazy { world.family { all(GameStateComponent) }.first() }

    override fun handle(event: Event?): Boolean {
        when (event) {
            is MapChangedEvent -> {
                mapTileLayer.clear()
                mapBg.clear()
                mapRenderer.map = event.map

                event.map.forEachLayer<TiledMapTileLayer> { layer ->
                    mapTileLayer.add(layer)
                }
                event.map.forEachLayer<TiledMapImageLayer> { layer ->
                    mapBg.add(layer)
                }
            }
        }
        return true
    }

    override fun onTick() {
        val gameStateCmp = gameStateEntity[GameStateComponent]
        // 1. Execute logic
        orthoCam.update()
        stage.viewport.apply()
        stage.act(deltaTime)

        mapRenderer.setView(orthoCam)
        mapRenderer.render()
        renderMap()

        // 2. Sort actors on the stage. This is for the stage.draw() path.
        stage.root.children.sort { a, b -> getZIndex(a).compareTo(getZIndex(b)) }

        lightEngine.update()

        if (!gameStateCmp.isLightingEnabled) {
            // 3. Default rendering path - uses the sorted actors on the stage
            stage.draw()
        } else {
            // 4. Advanced rendering path with lighting
            drawWithLightingEngine()
        }
        super.onTick()
    }

    override fun onTickEntity(entity: Entity) {
        val transformCmp = entity[TransformComponent]
        val gameStateCmp = gameStateEntity[GameStateComponent]

        entity.getOrNull(ImageComponent)?.let { imageCmp ->
            // Differentiate sizing logic based on whether the entity has a PhysicComponent
            val targetWidth: Float
            val targetHeight: Float

            if (entity has PhysicComponent) {
                // For entities with a PhysicComponent (e.g., player, mushroom),
                targetWidth = imageCmp.scaleX
                targetHeight = imageCmp.scaleY
            } else {
                // Update position for ImageComponent
                imageCmp.image.setPosition(transformCmp.position.x, transformCmp.position.y)
                // For entities without a PhysicComponent (e.g., map objects like fire),
                // transformCmp.width/height are the base sizes, and imageCmp.scaleX/Y are multipliers.
                targetWidth = transformCmp.width * imageCmp.scaleX
                targetHeight = transformCmp.height * imageCmp.scaleY
            }

            // TODO remove later when light engine is not switchable anymore
            // Apply flipping based on imageCmp.flipImage
            if (!gameStateCmp.isLightingEnabled) {
                val finalWidth = if (imageCmp.flipImage) -targetWidth else targetWidth
                imageCmp.image.setSize(finalWidth, targetHeight)
            } else {
                imageCmp.image.setSize(targetWidth, targetHeight)
            }
        }

        // Update position for ParticleComponent
        entity.getOrNull(ParticleComponent)?.let { particleCmp ->
            particleCmp.actor.setPosition(
                transformCmp.position.x + particleCmp.offsetX,
                transformCmp.position.y + particleCmp.offsetY,
            )
        }
    }

    private fun drawWithLightingEngine() {
        val playerEntity = world.family { any(PlayerComponent) }.first()
        val playerActor = playerEntity[ImageComponent].image
        lightEngine.renderLights(playerActor) { engine ->
            // The batch already has the light shader active here.

            // get all renderable entities
            val renderableEntities = mutableListOf<Entity>()
            world.family { any(ImageComponent, ParticleComponent) }.forEach {
                renderableEntities.add(it)
            }

            val sortedRenderableEntities =
                renderableEntities.sortedBy { it.zIndex }

            // Keep track of the current shader state
            var currentShaderIsDefault = false

            sortedRenderableEntities.forEach { entity ->
                val imageCmp = entity.getOrNull(ImageComponent)
                val shaderCmp = entity.getOrNull(ShaderRenderingComponent)
                val particleCmp = entity.getOrNull(ParticleComponent)

                if (shaderCmp?.normal != null && imageCmp != null) {
                    // This entity requires a custom shader
                    if (currentShaderIsDefault) {
                        engine.batch.flush()
                        engine.setShaderToEngineShader()
                        currentShaderIsDefault = false
                    }

                    if (shaderCmp.specular != null) {
                        engine.draw(
                            diffuse = shaderCmp.diffuse!!,
                            normals = shaderCmp.normal!!,
                            specular = shaderCmp.specular!!,
                            x = imageCmp.image.x,
                            y = imageCmp.image.y,
                            width = imageCmp.image.width,
                            height = imageCmp.image.height,
                            flipX = imageCmp.flipImage,
                        )
                    } else {
                        engine.draw(
                            diffuse = shaderCmp.diffuse!!,
                            normals = shaderCmp.normal!!,
                            x = imageCmp.image.x,
                            y = imageCmp.image.y,
                            width = imageCmp.image.width,
                            height = imageCmp.image.height,
                            flipX = imageCmp.flipImage,
                        )
                    }
                } else {
                    // This entity is a default entity (no special shaders) or a particle
                    if (!currentShaderIsDefault) {
                        engine.batch.flush()
                        engine.setShaderToDefaultShader()
                        currentShaderIsDefault = true
                    }

                    // Draw image if it exists
                    imageCmp?.let {
                        (it.image.drawable as? TextureRegionDrawable)?.let { tex ->
                            val region = tex.region
                            val x = it.image.x
                            val y = it.image.y
                            val width = it.image.width
                            val height = it.image.height

                            if (it.flipImage) {
                                engine.batch.draw(region, x + width, y, -width, height)
                            } else {
                                engine.batch.draw(region, x, y, width, height)
                            }
                        }
                    }

                    // Draw particles if they exist
                    particleCmp?.actor?.draw(engine.batch, 1f)
                }
            }
            // Ensure the engine shader is active at the end
            if (currentShaderIsDefault) {
                engine.batch.flush()
                engine.setShaderToEngineShader()
            }
        }
    }

    private fun renderMap() {
        AnimatedTiledMapTile.updateAnimationBaseTime() // is called to render animated tiles in the map
        mapRenderer.setView(orthoCam)
        // this is rendering the map
        stage.batch.use(orthoCam.combined) {
            mapBg.forEach {
                mapRenderer.renderImageLayer(it)
            }
            mapTileLayer.forEach {
                mapRenderer.renderTileLayer(it)
            }
        }
    }

    // Helper to get zIndex from an Actor's entity
    private fun getZIndex(actor: Actor): Int {
        val entity = actor.userObject as? Entity ?: return 0
        val imageZ = entity.getOrNull(ImageComponent)?.zIndex
        val particleZ = entity.getOrNull(ParticleComponent)?.zIndex
        return imageZ ?: particleZ ?: 0
    }

    // Helper to get zIndex directly from an entity
    private val Entity.zIndex: Int
        get() = this.getOrNull(ImageComponent)?.zIndex ?: this.getOrNull(ParticleComponent)?.zIndex ?: 0

    companion object {
        val logger = logger<RenderSystem>()
    }
}

enum class RenderLayer(
    val baseZIndex: Int,
) {
    BG_SKY(0),
    SKY(1000),
    BG_PARALLAX_1(2000),
    BG_PARALLAX_2(3000),
    BG_PARALLAX_3(4000),
    BG_PARALLAX_4(5000),
    MAP_BG(6000),
    TILES(7000),
    BG_MAP_OBJECTS(8000),
    CHARACTERS(9000),
    PROJECTILES(9500),
    FG_MAP_OBJECTS(10_000),
    FG_PARALLAX_1(11_000),
    FG_PARALLAX_2(12_000),
    UI(20_000),
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
        override val zIndex: Int, // layerZIndex + entity.zIndex
    ) : RenderableElement()

    data class EntityWithParticle(
        val entity: Entity,
        val particleCmp: ParticleComponent,
        override val zIndex: Int,
    ) : RenderableElement()
}
