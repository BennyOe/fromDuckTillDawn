package io.bennyoe.systems

import com.badlogic.gdx.graphics.g2d.Animation
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import io.bennyoe.components.AnimationComponent
import io.bennyoe.components.AnimationModel
import io.bennyoe.components.AnimationType
import io.bennyoe.components.AnimationVariant
import io.bennyoe.components.HealthComponent
import io.bennyoe.components.PlayerComponent
import ktx.log.logger

class DeadSystem : IteratingSystem(family { all(HealthComponent) }) {
    private var dying = false

    override fun onTickEntity(entity: Entity) {
        val healthCmp = entity[HealthComponent]
        val animationCmp = entity[AnimationComponent]

        if (entity hasNo PlayerComponent && healthCmp.isDead) {
            // TODO the whole animation part is only for testing. This has to be implemented properly with a state or ai system
            if (!dying) {
                dying = true
                animationCmp.mode = Animation.PlayMode.NORMAL
                animationCmp.stateTime = 0f
                animationCmp.nextAnimation(AnimationModel.ENEMY_MUSHROOM, AnimationType.DYING, AnimationVariant.FIRST)
            }
//            if (animationCmp.isAnimationFinished()) {
//                logger.debug { "Entity ${entity.id} dead, removing." }
//                entity.configure { it.remove() }
//            }
        }
    }

    companion object {
        val logger = logger<DeadSystem>()
    }
}
