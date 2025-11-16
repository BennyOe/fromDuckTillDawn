package io.bennyoe.systems

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import io.bennyoe.components.PhysicComponent
import io.bennyoe.components.ProjectileComponent
import io.bennyoe.components.ProjectileType
import io.bennyoe.components.TransformComponent
import io.bennyoe.components.animation.AnimationComponent
import io.bennyoe.components.animation.AnimationModel
import io.bennyoe.components.animation.MinotaurAnimation

class ProjectileSystem : IteratingSystem(family { all(ProjectileComponent) }) {
    override fun onTickEntity(entity: Entity) {
        val projectileCmp = entity[ProjectileComponent]

        if (projectileCmp.type == ProjectileType.ROCK) {
            if (projectileCmp.hitGround) {
                if (entity has PhysicComponent) {
                    val transformCmp = entity[TransformComponent]
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
    }
}
