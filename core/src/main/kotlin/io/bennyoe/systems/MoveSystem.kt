package io.bennyoe.systems

import com.badlogic.gdx.math.Vector2
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import io.bennyoe.components.AnimationComponent
import io.bennyoe.components.AnimationModel
import io.bennyoe.components.AnimationType
import io.bennyoe.components.AnimationVariant
import io.bennyoe.components.AttackComponent
import io.bennyoe.components.HasGroundContact
import io.bennyoe.components.MoveComponent
import io.bennyoe.components.PhysicComponent
import ktx.log.logger
import ktx.math.component1
import ktx.math.component2

private const val FALLING_THRESHOLD= -8f

private const val JUMP_FALLING_BOOST= 1.8f

class MoveSystem : IteratingSystem(family { all(PhysicComponent, MoveComponent, AnimationComponent) }) {

    private var currentAnimation: AnimationType = AnimationType.IDLE

    override fun onTickEntity(entity: Entity) {
        val moveCmp = entity[MoveComponent]
        val phyCmp = entity[PhysicComponent]
        val animCmp = entity[AnimationComponent]
        val attackCmp = entity[AttackComponent]
        val mass = phyCmp.body.mass
        val (_, velY) = phyCmp.body.linearVelocity

        // set new animation
        val newAnimation = when {
            entity hasNo HasGroundContact -> AnimationType.JUMP
            moveCmp.crouchMode && moveCmp.xDirection != 0f -> AnimationType.CROUCH_WALK
            moveCmp.crouchMode && moveCmp.xDirection == 0f -> AnimationType.CROUCH_IDLE
            moveCmp.xDirection != 0f -> AnimationType.WALK
            else -> AnimationType.IDLE
        }

        // only update when animation changed
        updateAnimation(animCmp, newAnimation)

        calculateJumpProperties(entity, velY, moveCmp)

        // set impulses
        if ((moveCmp.jumpCounter > 2) ||
            (moveCmp.jumpCounter == 2 &&
                moveCmp.jumpRequest &&
                (moveCmp.timeSinceGrounded > moveCmp.maxCoyoteTime))
        ) {
            moveCmp.jumpRequest = false
        }
        if (moveCmp.jumpRequest) {
            // TODO sometimes when walking off a platform and jumping in the air, the first jump is counted twice and no airjump is possible. I
            //  think this is because the jump button is recognized in multiple frames
            moveCmp.jumpCounter++
            var impulseY = mass * moveCmp.jumpBoost

            impulseY = if (phyCmp.body.linearVelocity.y < FALLING_THRESHOLD) {
                impulseY * JUMP_FALLING_BOOST
            } else {
                impulseY
            }
            phyCmp.impulse = Vector2(0f, impulseY)
        }


        // ignore when bash is active
        if (!attackCmp.bashActive) {
            val desiredVelocityX = moveCmp.speed * moveCmp.xDirection
            phyCmp.body.linearVelocity = Vector2(desiredVelocityX, phyCmp.body.linearVelocity.y)
        }


        // flip image if necessary
        if (moveCmp.xDirection != 0f) {
            animCmp.flipImage = moveCmp.xDirection < 0
        }
    }

    private fun calculateJumpProperties(entity: Entity, velY: Float, moveCmp: MoveComponent) {
        if (entity has HasGroundContact && velY <= 0) {
            moveCmp.timeSinceGrounded = 0f
            moveCmp.jumpCounter = 0
        } else {
            moveCmp.timeSinceGrounded += deltaTime
        }
    }

    private fun updateAnimation(animCmp: AnimationComponent, newAnimation: AnimationType) {
        if (currentAnimation != newAnimation) {
            animCmp.nextAnimation(AnimationModel.PLAYER_DAWN, newAnimation, AnimationVariant.FIRST)
            currentAnimation = newAnimation
        }
    }

    companion object {
        private val LOG = logger<MoveSystem>()
    }
}
