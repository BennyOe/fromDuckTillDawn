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
import io.bennyoe.components.RainComponent
import io.bennyoe.components.SkyComponent
import io.bennyoe.components.SkyComponentType
import io.bennyoe.components.TransformComponent

/**
 * Synchronizes visual components (Images, Particles) from the TransformComponent.
 *
 * This system is the "Source of Truth" for *visual* placement for entities without [PhysicComponent].
 * It reads from the TransformComponent and updates the corresponding Scene2D-Actors.
 *
 * It handles two main cases for positioning:
 * 1.  **Static Entities (without PhysicComponent):**
 * - `TransformComponent.position` is the BOTTOM-LEFT corner (as placed in Tiled).
 * - The Image is positioned directly at this coordinate.
 * 2.  **Special Cases (Sky, Rain):**
 * - Full-screen elements like sky or rain are positioned relative to the camera's viewport,
 * not based on the entity's TransformComponent position.
 *
 * @param stage The LibGDX stage, used to access the camera.
 */
class TransformVisualSyncSystem(
    stage: Stage = inject("stage"),
) : IteratingSystem(
        family {
            all(TransformComponent)
            none(PhysicComponent)
        },
    ) {
    private val orthoCam = stage.camera as OrthographicCamera

    override fun onTickEntity(entity: Entity) {
        val transformCmp = entity[TransformComponent]

        // Sync ImageComponent if present
        entity.getOrNull(ImageComponent)?.let { imageCmp ->
            syncImage(entity, transformCmp, imageCmp)
        }

        // Sync ParticleComponent if present
        entity.getOrNull(ParticleComponent)?.let { particleCmp ->
            syncParticle(entity, transformCmp, particleCmp)
        }
    }

    /**
     * Updates the position and size of the Image actor based on the entity's status.
     */
    private fun syncImage(
        entity: Entity,
        transformCmp: TransformComponent,
        imageCmp: ImageComponent,
    ) {
        val skyCmp = entity.getOrNull(SkyComponent)

        val targetWidth: Float
        val targetHeight: Float

        // Case 2: Full-screen backgrounds (Sky, Stars)
        if (skyCmp != null && (skyCmp.type == SkyComponentType.SKY || skyCmp.type == SkyComponentType.STARS)) {
            val vw = orthoCam.viewportWidth * orthoCam.zoom
            val vh = orthoCam.viewportHeight * orthoCam.zoom
            val camX = orthoCam.position.x - vw * 0.5f
            val camY = orthoCam.position.y - vh * 0.5f

            imageCmp.image.setPosition(camX, camY)
            targetWidth = vw
            targetHeight = vh
        } else {
            // Case 1: Standard entities (without physics)
            targetWidth = transformCmp.width * imageCmp.scaleX
            targetHeight = transformCmp.height * imageCmp.scaleY

            imageCmp.image.setPosition(
                transformCmp.position.x,
                transformCmp.position.y,
            )
        }
        imageCmp.image.setSize(targetWidth, targetHeight)
    }

    /**
     * Updates the position of the Particle actor based on the entity's status.
     */
    private fun syncParticle(
        entity: Entity,
        transformCmp: TransformComponent,
        particleCmp: ParticleComponent,
    ) {
        val skyCmp = entity.getOrNull(SkyComponent)

        // Case 1: Full-screen particles (Rain, Shooting Stars)
        if (skyCmp != null || entity has RainComponent) {
            val vw = orthoCam.viewportWidth * orthoCam.zoom
            val vh = orthoCam.viewportHeight * orthoCam.zoom
            val camX = orthoCam.position.x - vw * 0.5f - 15f
            val camY = orthoCam.position.y + vh * 0.5f + 10f
            particleCmp.actor.setPosition(camX + particleCmp.offsetX, camY + particleCmp.offsetY)
        } else {
            // Case 2: World-space particles (e.g., Fire)
            // Assumes transformCmp.position is BOTTOM-LEFT,
            // which is correct for static entities (without physics).
            particleCmp.actor.setPosition(
                transformCmp.position.x + particleCmp.offsetX,
                transformCmp.position.y + particleCmp.offsetY,
            )
        }
    }
}
