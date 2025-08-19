package io.bennyoe.systems

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.maps.tiled.TiledMapImageLayer
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer
import com.badlogic.gdx.maps.tiled.tiles.AnimatedTiledMapTile
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
) : IteratingSystem(
        family { all(TransformComponent).any(ImageComponent, ParticleComponent) },
        enabled = !SHOW_ONLY_DEBUG,
    ),
    EventListener {
    private val mapRenderer = OrthogonalTiledMapRenderer(null, UNIT_SCALE, stage.batch)
    private val orthoCam = stage.camera as OrthographicCamera
    private val gameStateEntity by lazy { world.family { all(GameStateComponent) }.first() }

    // Storage for map layers from MapChangedEvent
    private val mapTileLayers = mutableListOf<TiledMapTileLayer>()
    private val mapImageLayers = mutableListOf<TiledMapImageLayer>()

    // The unified render queue - rebuilt each frame
    private val renderQueue = mutableListOf<RenderableElement>()

    override fun handle(event: Event?): Boolean {
        when (event) {
            is MapChangedEvent -> {
                // Clear and store map layers with their zIndex
                mapTileLayers.clear()
                mapImageLayers.clear()
                mapRenderer.map = event.map

                event.map.forEachLayer<TiledMapTileLayer> { layer ->
                    mapTileLayers.add(layer)
                }
                event.map.forEachLayer<TiledMapImageLayer> { layer ->
                    mapImageLayers.add(layer)
                }

                return true
            }
        }
        return false
    }

    override fun onTick() {
        val gameStateCmp = gameStateEntity[GameStateComponent]

        // 1. Update camera and stage
        orthoCam.update()
        stage.viewport.apply()
        stage.act(deltaTime)

        // 2. Update all entity transforms/positions via onTickEntity
        super.onTick() // This calls onTickEntity for each entity

        // 3. Build the render queue with everything sorted by zIndex
        buildRenderQueue()

        // 4. Update lighting system
        lightEngine.update()

        // 5. Render everything in the correct order
        if (!gameStateCmp.isLightingEnabled) {
            renderWithoutLighting()
        } else {
            renderWithLighting()
        }
    }

    override fun onTickEntity(entity: Entity) {
        val transformCmp = entity[TransformComponent]

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

            imageCmp.image.setSize(targetWidth, targetHeight)
        }

        // Update position for ParticleComponent
        entity.getOrNull(ParticleComponent)?.let { particleCmp ->
            particleCmp.actor.setPosition(
                transformCmp.position.x + particleCmp.offsetX,
                transformCmp.position.y + particleCmp.offsetY,
            )
        }
    }

    private fun buildRenderQueue() {
        renderQueue.clear()

        // 1. Add all map image layers
        mapImageLayers.forEach { layer ->
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
            entity.getOrNull(ImageComponent)?.let { imageCmp ->
                renderQueue.add(
                    RenderableElement.EntityWithImage(
                        entity = entity,
                        imageCmp = imageCmp,
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

    private fun renderWithoutLighting() {
        // Simple rendering without lighting - everything in order
        AnimatedTiledMapTile.updateAnimationBaseTime()
        mapRenderer.setView(orthoCam)

        stage.batch.use(orthoCam.combined) {
            renderQueue.forEach { renderable ->
                when (renderable) {
                    is RenderableElement.TileLayer -> {
                        mapRenderer.renderTileLayer(renderable.layer)
                    }

                    is RenderableElement.ImageLayer -> {
                        mapRenderer.renderImageLayer(renderable.layer)
                    }

                    is RenderableElement.EntityWithImage -> {
                        if (renderable.imageCmp.flipImage) {
                            // Draw flipped by adjusting position and using negative width
                            val texture = renderable.imageCmp.image.drawable as? TextureRegionDrawable
                            val region = texture?.region
                            if (region != null) {
                                it.draw(
                                    region,
                                    renderable.imageCmp.image.x + renderable.imageCmp.image.width,
                                    renderable.imageCmp.image.y,
                                    -renderable.imageCmp.image.width,
                                    renderable.imageCmp.image.height,
                                )
                            }
                        } else {
                            // Draw normally
                            renderable.imageCmp.image.draw(it, 1f)
                        }
                    }

                    is RenderableElement.EntityWithParticle -> {
                        renderable.particleCmp.actor.draw(it, 1f)
                    }
                }
            }
        }
    }

    private fun renderWithLighting() {
        AnimatedTiledMapTile.updateAnimationBaseTime()
        mapRenderer.setView(orthoCam)

        val playerEntity = world.family { any(PlayerComponent) }.first()
        val playerActor = playerEntity[ImageComponent].image

        lightEngine.renderLights(playerActor) { engine ->
            var currentShader = ShaderType.NONE
            engine.batch.projectionMatrix = orthoCam.combined

            renderQueue.forEach { renderable ->
                currentShader =
                    when (renderable) {
                        is RenderableElement.TileLayer -> {
                            switchToDefaultShaderIfNeeded(engine, currentShader)
                            mapRenderer.renderTileLayer(renderable.layer)
                            ShaderType.DEFAULT
                        }

                        is RenderableElement.ImageLayer -> {
                            switchToDefaultShaderIfNeeded(engine, currentShader)
                            mapRenderer.renderImageLayer(renderable.layer)
                            ShaderType.DEFAULT
                        }

                        is RenderableElement.EntityWithImage -> {
                            renderEntityWithCorrectShader(engine, renderable.entity, renderable.imageCmp, currentShader)
                        }

                        is RenderableElement.EntityWithParticle -> {
                            switchToDefaultShaderIfNeeded(engine, currentShader)
                            renderable.particleCmp.actor.draw(engine.batch, 1f)
                            ShaderType.DEFAULT
                        }
                    }
            }
        }
    }

    private fun switchToDefaultShaderIfNeeded(
        engine: Scene2dLightEngine,
        currentShader: ShaderType,
    ): ShaderType {
        if (currentShader != ShaderType.DEFAULT) {
            engine.batch.flush()
            engine.setShaderToDefaultShader()
        }
        return ShaderType.DEFAULT
    }

    private fun switchToLightingShaderIfNeeded(
        engine: Scene2dLightEngine,
        currentShader: ShaderType,
    ): ShaderType {
        if (currentShader != ShaderType.LIGHTING) {
            engine.batch.flush()
            engine.setShaderToEngineShader()
        }
        return ShaderType.LIGHTING
    }

    private fun renderEntityWithCorrectShader(
        engine: Scene2dLightEngine,
        entity: Entity,
        imageCmp: ImageComponent,
        currentShader: ShaderType,
    ): ShaderType {
        val shaderCmp = entity.getOrNull(ShaderRenderingComponent)
        val particleCmp = entity.getOrNull(ParticleComponent)

        val newShaderType =
            if (shaderCmp?.normal != null) {
                // Entity needs lighting shader
                val updatedShader = switchToLightingShaderIfNeeded(engine, currentShader)
                renderWithNormalMapping(engine, imageCmp, shaderCmp)
                updatedShader
            } else {
                // Entity uses default shader
                val updatedShader = switchToDefaultShaderIfNeeded(engine, currentShader)
                renderWithBasicShader(engine, imageCmp)
                updatedShader
            }

        // Draw particles if present (use current shader)
        particleCmp?.actor?.draw(engine.batch, 1f)

        return newShaderType
    }

    private fun renderWithNormalMapping(
        engine: Scene2dLightEngine,
        imageCmp: ImageComponent,
        shaderCmp: ShaderRenderingComponent,
    ) {
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
    }

    private fun renderWithBasicShader(
        engine: Scene2dLightEngine,
        imageCmp: ImageComponent,
    ) {
        val texture = imageCmp.image.drawable as? TextureRegionDrawable ?: return
        val region = texture.region

        if (imageCmp.flipImage) {
            engine.batch.draw(
                region,
                imageCmp.image.x + imageCmp.image.width,
                imageCmp.image.y,
                -imageCmp.image.width,
                imageCmp.image.height,
            )
        } else {
            engine.batch.draw(
                region,
                imageCmp.image.x,
                imageCmp.image.y,
                imageCmp.image.width,
                imageCmp.image.height,
            )
        }
    }

    companion object {
        val logger = logger<RenderSystem>()
    }
}

enum class ShaderType {
    NONE, // Initial state
    DEFAULT, // Default/basic shader
    LIGHTING, // Engine lighting shader
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
        override val zIndex: Int,
    ) : RenderableElement()

    data class EntityWithParticle(
        val entity: Entity,
        val particleCmp: ParticleComponent,
        override val zIndex: Int,
    ) : RenderableElement()
}
