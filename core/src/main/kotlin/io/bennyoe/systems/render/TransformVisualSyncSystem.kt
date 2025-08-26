package io.bennyoe.systems.render

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import com.github.quillraven.fleks.World.Companion.inject
import io.bennyoe.components.ImageComponent
import io.bennyoe.components.ParticleComponent
import io.bennyoe.components.PhysicComponent
import io.bennyoe.components.SkyComponent
import io.bennyoe.components.SkyComponentType
import io.bennyoe.components.TransformComponent

/**
 * System that synchronizes the visual representation of entities with their transform data.
 *
 * - For entities with a `PhysicComponent`, it updates the image size based on the image's scale.
 * - For background entities (`SkyComponent` of type `SKY` or `STARS`), it aligns the image to the camera's viewport.
 * - For all other entities, it positions and sizes the image based on the `TransformComponent` and `ImageComponent`.
 * - If a `ParticleComponent` is present, it updates the particle actor's position and size accordingly.
 *
 * @param stage The LibGDX stage used to access the camera for viewport calculations.
 */
class TransformVisualSyncSystem(
    stage: Stage = inject("stage"),
) : IteratingSystem(family { all(TransformComponent) }) {
    private val orthoCam = stage.camera as OrthographicCamera

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
}
