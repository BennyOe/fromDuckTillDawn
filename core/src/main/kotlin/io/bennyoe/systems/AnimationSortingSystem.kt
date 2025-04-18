package io.bennyoe.systems

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import io.bennyoe.components.AnimationCollectionComponent
import io.bennyoe.components.AnimationComponent
import io.bennyoe.components.AnimationModel
import io.bennyoe.components.AnimationType
import io.bennyoe.components.AnimationVariant

class AnimationSortingSystem : IteratingSystem(family { all(AnimationCollectionComponent) }, enabled = true) {

    override fun onTickEntity(entity: Entity) {
        val animColCmp = entity[AnimationCollectionComponent]
        val animCmp = entity[AnimationComponent]
        animColCmp.animations.sortByDescending { it.priority }
        val newAnimation = animColCmp.animations.firstOrNull()

        // change animation if canInterrupt
        if (newAnimation != null &&
            animCmp.nextAnimationType == AnimationType.NONE &&
            animCmp.currentAnimation != newAnimation &&
            animCmp.currentAnimation.canInterrupt
        ) {

//        logger.debug { "list ${animColCmp.animations.firstOrNull()}}" }
            animCmp.nextAnimation(AnimationModel.PLAYER_DAWN, newAnimation, AnimationVariant.FIRST)
            animCmp.currentAnimation = newAnimation
        }

        // Attack and Bash reset
        if (animCmp.nextAnimationType == AnimationType.NONE &&
            !animCmp.currentAnimation.canInterrupt &&
            animCmp.animation.isAnimationFinished(animCmp.stateTime)){
            logger.debug { "NONE" }
            animCmp.currentAnimation = AnimationType.NONE
        }
        animColCmp.animations.clear()

    }

    companion object {
        val logger = ktx.log.logger<AnimationSortingSystem>()
    }
}
