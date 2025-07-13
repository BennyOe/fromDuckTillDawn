package io.bennyoe.systems

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.github.bennyOe.gdxNormalLight.core.Scene2dLightEngine
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
import ktx.log.logger

class RenderSystem(
    private val stage: Stage = inject("stage"),
    private val lightEngine: Scene2dLightEngine = inject("lightEngine"),
) : IteratingSystem(family { all(TransformComponent).any(ImageComponent, ParticleComponent) }, enabled = !SHOW_ONLY_DEBUG) {
    private val orthoCam = stage.camera as OrthographicCamera
    private val gameStateEntity by lazy { world.family { all(GameStateComponent) }.first() }

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

    override fun onTick() {
        val gameStateCmp = gameStateEntity[GameStateComponent]
        // 1. Execute logic
        orthoCam.update()
        stage.viewport.apply()
        stage.act(deltaTime)

        // 2. Sort actors on the stage. This is for the stage.draw() path.
        // You correctly identified that the old logic here was broken. This is the fix.
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

    companion object {
        val logger = logger<RenderSystem>()
    }
}
