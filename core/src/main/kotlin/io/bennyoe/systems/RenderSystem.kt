package io.bennyoe.systems

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.github.bennyOe.gdxNormalLight.core.Scene2dLightEngine
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import com.github.quillraven.fleks.World.Companion.inject
import io.bennyoe.components.GameStateComponent
import io.bennyoe.components.ImageComponent
import io.bennyoe.components.PhysicComponent
import io.bennyoe.components.PlayerComponent
import io.bennyoe.components.ShaderRenderingComponent
import io.bennyoe.components.TransformComponent
import io.bennyoe.config.GameConstants.SHOW_ONLY_DEBUG
import ktx.log.logger

class RenderSystem(
    private val stage: Stage = inject("stage"),
    private val lightEngine: Scene2dLightEngine = inject("lightEngine"),
) : IteratingSystem(family { any(ImageComponent, TransformComponent) }, enabled = !SHOW_ONLY_DEBUG) {
    private val orthoCam = stage.camera as OrthographicCamera
    private val gameStateEntity by lazy { world.family { all(GameStateComponent) }.first() }

    override fun onTick() {
        val gameStateCmp = gameStateEntity[GameStateComponent]
        // 1. Execute logic
        orthoCam.update()
        stage.viewport.apply()
        stage.act(deltaTime)

        // TODO sorting the actors every frame is not performant. For now it is ok but later we should think of actorGroups (background, middle,
        //  foreground, ui) or implementing a dirty flag, when a zIndex has changed.
        stage.root.children.sort { a, b ->
            val aEntity = a.userObject as? Entity
            val bEntity = b.userObject as? Entity
            val aImageCmp = aEntity?.getOrNull(ImageComponent)
            val bImageCmp = bEntity?.getOrNull(ImageComponent)

            if (aEntity != null && bEntity != null) {
                if (aImageCmp != null && bImageCmp != null) {
                    aImageCmp.zIndex.compareTo(bImageCmp.zIndex)
                } else {
                    0
                }
            } else {
                0
            }
        }

        lightEngine.update()

        if (!gameStateCmp.isLightingEnabled) {
            stage.draw()
        } else {
            // 2. Call LightEngine and draw the scene
            drawWithLightingEngine()
        }
        super.onTick()
    }

    override fun onTickEntity(entity: Entity) {
        val imageCmp = entity.getOrNull(ImageComponent) ?: return
        val transformCmp = entity.getOrNull(TransformComponent) ?: return
        val gameStateCmp = gameStateEntity[GameStateComponent]

        // Position the image actor based on the transform component
        imageCmp.image.setPosition(transformCmp.position.x, transformCmp.position.y)

        // Differentiate sizing logic based on whether the entity has a PhysicComponent
        val targetWidth: Float
        val targetHeight: Float

        if (entity has PhysicComponent) {
            // For entities with a PhysicComponent (e.g., player, mushroom),
            targetWidth = imageCmp.scaleX
            targetHeight = imageCmp.scaleY
        } else {
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

    private fun drawWithLightingEngine() {
        val playerEntity = world.family { any(PlayerComponent) }.first()
        val playerActor = playerEntity[ImageComponent].image
        lightEngine.renderLights(playerActor) { engine ->

            // The batch already has the light shader active here.
            val renderableEntities = mutableListOf<Entity>()
            world.family { all(ImageComponent) }.forEach {
                renderableEntities.add(it)
            }
            val sortedRenderableEntities = renderableEntities.sortedBy { it[ImageComponent].image.zIndex }

            // Keep track of the current shader state
            var currentShaderIsDefault = false

            sortedRenderableEntities.forEach { entity ->
                val imageCmp = entity[ImageComponent]
                val shaderCmp = entity.getOrNull(ShaderRenderingComponent)

                if (shaderCmp?.normal != null) {
                    // This entity requires a custom shader (normal or normal+specular)
                    if (currentShaderIsDefault) {
                        engine.batch.flush() // Flush any default shader draws
                        engine.setShaderToEngineShader() // Switch back to engine shader
                        currentShaderIsDefault = false
                    }

                    if (shaderCmp.specular != null) {
                        // Render with normal and specular maps
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
                        // Render with only normal maps
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
                    // This entity is a default entity (no special shaders)
                    if (!currentShaderIsDefault) {
                        engine.batch.flush() // Flush any custom shader draws
                        engine.setShaderToDefaultShader() // Switch to default shader
                        currentShaderIsDefault = true
                    }

                    val drawable = imageCmp.image.drawable as? TextureRegionDrawable ?: return@forEach
                    val region = drawable.region

                    val x = imageCmp.image.x
                    val y = imageCmp.image.y
                    val width = imageCmp.image.width
                    val height = imageCmp.image.height

                    if (imageCmp.flipImage) {
                        engine.batch.draw(region, x + width, y, -width, height)
                    } else {
                        engine.batch.draw(region, x, y, width, height)
                    }
                }
            }
            // Ensure the engine shader is active at the end if it was switched to default
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
