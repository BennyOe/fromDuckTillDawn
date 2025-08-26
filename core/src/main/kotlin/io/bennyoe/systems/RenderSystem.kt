package io.bennyoe.systems

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.glutils.ShaderProgram
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
import io.bennyoe.components.SkyComponent
import io.bennyoe.components.SkyComponentType
import io.bennyoe.components.TimeOfDay
import io.bennyoe.components.TransformComponent
import io.bennyoe.config.GameConstants.SHOW_ONLY_DEBUG
import io.bennyoe.config.GameConstants.UNIT_SCALE
import io.bennyoe.event.MapChangedEvent
import io.bennyoe.lightEngine.core.Scene2dLightEngine
import io.bennyoe.utility.findImageLayerDeep
import io.bennyoe.utility.findTileLayerDeep
import io.bennyoe.utility.setUniformAny
import ktx.graphics.use
import ktx.log.logger

// TODO refactor
class RenderSystem(
    private val stage: Stage = inject("stage"),
    private val lightEngine: Scene2dLightEngine = inject("lightEngine"),
) : IteratingSystem(
        family { all(TransformComponent).any(ImageComponent, ParticleComponent) },
        enabled = !SHOW_ONLY_DEBUG,
    ),
    EventListener {
    private var continuousTime = 0f
    private val mapRenderer = OrthogonalTiledMapRenderer(null, UNIT_SCALE, stage.batch)
    private val orthoCam = stage.camera as OrthographicCamera
    private val gameStateCmp by lazy { world.family { all(GameStateComponent) }.first()[GameStateComponent] }

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
        val skyCmp = entity.getOrNull(SkyComponent)

        entity.getOrNull(ImageComponent)?.let { imageCmp ->
            // Differentiate sizing logic based on whether the entity has a PhysicComponent
            val targetWidth: Float
            val targetHeight: Float

            if (entity has PhysicComponent) {
                // For entities with a PhysicComponent (e.g., player, mushroom),
                targetWidth = imageCmp.scaleX
                targetHeight = imageCmp.scaleY
            } else {
                // We now tie the background's position and size directly to the camera's view.
                if (skyCmp != null && (skyCmp.type == SkyComponentType.SKY || skyCmp.type == SkyComponentType.STARS)) {
                    val vw = orthoCam.viewportWidth * orthoCam.zoom
                    val vh = orthoCam.viewportHeight * orthoCam.zoom

                    val camX = orthoCam.position.x - vw * 0.5f
                    val camY = orthoCam.position.y - vh * 0.5f

                    imageCmp.image.setPosition(camX, camY)

                    targetWidth = vw
                    targetHeight = vh
                } else {
                    imageCmp.image.setPosition(transformCmp.position.x, transformCmp.position.y)
                    targetWidth = transformCmp.width * imageCmp.scaleX
                    targetHeight = transformCmp.height * imageCmp.scaleY
                }
            }
            imageCmp.image.setSize(targetWidth, targetHeight)
        }

        // Update position for ParticleComponent
        entity.getOrNull(ParticleComponent)?.let { particleCmp ->
            if (skyCmp != null) {
                val vw = orthoCam.viewportWidth * orthoCam.zoom
                val vh = orthoCam.viewportHeight * orthoCam.zoom
                val camX = orthoCam.position.x - vw * 0.5f
                val camY = orthoCam.position.y + vh * 0.5f
                particleCmp.actor.setPosition(camX + particleCmp.offsetX, camY + particleCmp.offsetY)
                particleCmp.actor.setSize(vw, vh)
            } else {
                particleCmp.actor.setPosition(
                    transformCmp.position.x + particleCmp.offsetX,
                    transformCmp.position.y + particleCmp.offsetY,
                )
            }
        }
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
            entity.getOrNull(ImageComponent)?.let { imageCmp ->
                renderQueue.add(
                    RenderableElement.EntityWithImage(
                        entity = entity,
                        imageCmp = imageCmp,
                        shaderRenderingCmp = shaderRenderingCmp,
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
                        if (renderable.shaderRenderingCmp?.shader != null) {
                            stage.batch.shader = renderable.shaderRenderingCmp.shader
                            updateUniforms(renderable.shaderRenderingCmp)
                            configureShaderNoiseTexture(renderable.shaderRenderingCmp)
                        }
                        if (renderable.imageCmp.flipImage) {
                            // Draw flipped by adjusting position and using negative width
                            val texture = renderable.imageCmp.image.drawable as? TextureRegionDrawable
                            val region = texture?.region
                            if (region != null) {
                                val oldColor = it.color.cpy()
                                it.color = renderable.imageCmp.image.color
                                it.draw(
                                    region,
                                    renderable.imageCmp.image.x + renderable.imageCmp.image.width,
                                    renderable.imageCmp.image.y,
                                    -renderable.imageCmp.image.width,
                                    renderable.imageCmp.image.height,
                                )
                                it.color = oldColor
                            }
                        } else {
                            // Draw normally
                            renderable.imageCmp.image.draw(it, 1f)
                        }
                        stage.batch.shader = null
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
                            // tint the farest background at night dark
                            // TODO this has to be generic
                            if (gameStateCmp.getTimeOfDay() == TimeOfDay.NIGHT && renderable.layer.name == "backgroundMountain") {
                                mapRenderer.batch.color = Color(0.6f, 0.6f, 0.6f, 1f)
                            } else {
                                mapRenderer.batch.color = Color.WHITE
                            }

                            switchToDefaultShaderIfNeeded(engine, currentShader)
                            mapRenderer.renderImageLayer(renderable.layer)
                            ShaderType.DEFAULT
                        }

                        is RenderableElement.EntityWithImage -> {
                            renderEntityWithCorrectShader(engine, renderable.entity, renderable.imageCmp, currentShader)
                        }

                        is RenderableElement.EntityWithParticle -> {
                            switchToDefaultShaderIfNeeded(engine, currentShader)
                            if (!renderable.particleCmp.enabled) return@forEach
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

    private fun switchToCustomShaderIfNeeded(
        engine: Scene2dLightEngine,
        customShader: ShaderProgram,
    ) {
        engine.batch.flush()
        engine.setShaderToCustomShader(customShader)
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
            } else if (shaderCmp?.shader != null) {
                val desiredShader = shaderCmp.shader!!
                if (currentShader != ShaderType.CUSTOM || engine.batch.shader != desiredShader) {
                    switchToCustomShaderIfNeeded(engine, desiredShader)
                }
                updateUniforms(shaderCmp)

                // if shaderCmp has a noise texture, use it
                configureShaderNoiseTexture(shaderCmp)

                // pass the correct texture coordinates to the shader (without this, the coordinates are the screen, not the texture coordinates)
                val drawable = imageCmp.image.drawable as? TextureRegionDrawable
                if (drawable != null) {
                    val region = drawable.region
                    shaderCmp.shader?.setUniformf("u_texCoord_min", region.u, region.v)
                    shaderCmp.shader?.setUniformf("u_texCoord_max", region.u2, region.v2)
                }

                drawRegion(engine, imageCmp)
                ShaderType.CUSTOM
            } else {
                // Entity uses default shader
                val updatedShader = switchToDefaultShaderIfNeeded(engine, currentShader)
                drawRegion(engine, imageCmp)
                updatedShader
            }

        // Draw particles if present (use current shader)
        particleCmp?.actor?.draw(engine.batch, 1f)

        return newShaderType
    }

    private fun configureShaderNoiseTexture(shaderCmp: ShaderRenderingComponent) {
        if (shaderCmp.noiseTexture != null) {
            Gdx.gl.glActiveTexture(GL20.GL_TEXTURE1)
            shaderCmp.noiseTexture!!.bind()
            shaderCmp.shader?.setUniformi("u_noiseTexture", 1)
            Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0)
        }
    }

    private fun updateUniforms(shaderCmp: ShaderRenderingComponent) {
        val shader = shaderCmp.shader!!

        // time sensitive uniforms. Set every frame
        shader.setUniformf("u_time", gameStateCmp.timeOfDay)
        shader.setUniformf("u_continuousTime", continuousTime)

        for ((name, value) in shaderCmp.uniforms) {
            setUniformAny(shader, name, value)
        }
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

    private fun drawRegion(
        engine: Scene2dLightEngine,
        imageCmp: ImageComponent,
    ) {
        val texture = imageCmp.image.drawable as? TextureRegionDrawable ?: return
        val region = texture.region
        val oldColor = engine.batch.color.cpy()
        engine.batch.color = imageCmp.image.color

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
        engine.batch.color = oldColor
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
        override val zIndex: Int,
    ) : RenderableElement()

    data class EntityWithParticle(
        val entity: Entity,
        val particleCmp: ParticleComponent,
        override val zIndex: Int,
    ) : RenderableElement()
}
