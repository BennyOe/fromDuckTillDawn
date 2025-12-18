package io.bennyoe.systems

import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import com.github.quillraven.fleks.World.Companion.inject
import io.bennyoe.components.PhysicComponent
import io.bennyoe.components.ProjectileComponent
import io.bennyoe.components.ProjectileType
import io.bennyoe.components.TransformComponent
import io.bennyoe.components.animation.AnimationComponent
import io.bennyoe.components.animation.AnimationModel
import io.bennyoe.components.animation.MinotaurAnimation
import io.bennyoe.utility.getViewportDimensions
import ktx.log.logger

class ProjectileSystem(
    val stage: Stage = inject("stage"),
) : IteratingSystem(family { all(ProjectileComponent) }) {
    override fun onTickEntity(entity: Entity) {
        val projectileCmp = entity[ProjectileComponent]
        val transformCmp = entity[TransformComponent]

        when (projectileCmp.type) {
            ProjectileType.ROCK -> {
                if (projectileCmp.hitGround) {
                    if (entity has PhysicComponent) {
                        // Phase 1: Initial hit. The entity still has a PhysicComponent.
                        // Start the animation and remove physics to stop it from moving/colliding.

                        // update the transformCmp from middle to left bottom
                        transformCmp.position.x -= transformCmp.width * 0.5f
                        transformCmp.position.y -= transformCmp.height * 0.5f

                        val animationCmp = AnimationComponent()
                        animationCmp.animationModel = AnimationModel.ENEMY_MINOTAUR
                        animationCmp.nextAnimation(MinotaurAnimation.ROCK_BREAK)
                        entity.configure {
                            it += animationCmp
                            it -= PhysicComponent
                        }
                    } else {
                        // Phase 2: Animation is playing (PhysicComponent is already gone).
                        // Wait for the animation to finish, then remove the entity.
                        val animCmp = entity.getOrNull(AnimationComponent)
                        if (animCmp != null && animCmp.isAnimationFinished()) {
                            world -= entity
                        }
                    }
                }
            }

            ProjectileType.SHOCKWAVE -> {
                val viewportDimensions = getViewportDimensions(stage)
                if (transformCmp.position.x !in viewportDimensions.left - 4f..viewportDimensions.right + 4f) {
                    world -= entity
                    logger.debug { "shockwave destroyed" }
                }
            }
        }
    }

    companion object {
        val logger = logger<ProjectileSystem>()
    }
}
