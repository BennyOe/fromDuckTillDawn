package io.bennyoe.systems

import com.badlogic.gdx.math.Vector2
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import io.bennyoe.components.AnimationComponent
import io.bennyoe.components.AnimationType
import io.bennyoe.components.HasGroundContact
import io.bennyoe.components.MoveComponent
import io.bennyoe.components.PhysicComponent
import ktx.log.logger
import ktx.math.component1
import ktx.math.component2

class MoveSystem : IteratingSystem(family { all(PhysicComponent, MoveComponent, AnimationComponent) }) {

    private var currentAnimation: AnimationType = AnimationType.IDLE

    override fun onTickEntity(entity: Entity) {
        val moveCmp = entity[MoveComponent]
        val phyCmp = entity[PhysicComponent]
        val animCmp = entity[AnimationComponent]
        val mass = phyCmp.body.mass
        val (velX, velY) = phyCmp.body.linearVelocity

        // set new animation
        val newAnimation = when {
            entity hasNo HasGroundContact -> AnimationType.JUMP
            moveCmp.attack -> AnimationType.ATTACK
            moveCmp.xDirection != 0f -> AnimationType.WALK
            else -> AnimationType.IDLE
        }

        // only update when animation changed
        updateAnimation(animCmp, newAnimation)

        if (entity has HasGroundContact && velY <= 0) {
            if (moveCmp.jumpCounter > 0) {
                LOG.debug { "Resetting jumpCounter from ${moveCmp.jumpCounter} due to ground contact!" }
            }
            moveCmp.timeSinceGrounded = 0f
            moveCmp.jumpCounter = 0
        } else {
            moveCmp.timeSinceGrounded += deltaTime
        }

        // set impulses
        if ((moveCmp.jumpCounter > 2) ||
            (moveCmp.jumpCounter == 2 && moveCmp.jumpRequest && (moveCmp.timeSinceGrounded > moveCmp.maxCoyoteTime))) {
            LOG.debug { "TOO MANY JUMPS!!!" }
            moveCmp.jumpRequest = false
        }

        if (moveCmp.jumpRequest) {
            moveCmp.jumpCounter++
            LOG.debug { "time: ${moveCmp.maxCoyoteTime - moveCmp.timeSinceGrounded}" }
            val impulseY = mass * moveCmp.jumpBoost * 7
            phyCmp.impulse = Vector2(0f, impulseY)
            LOG.debug { "counter: ${moveCmp.jumpCounter}" }
        }

        val desiredVelocityX = moveCmp.speed * moveCmp.xDirection
        phyCmp.body.linearVelocity = Vector2(desiredVelocityX, phyCmp.body.linearVelocity.y)


        // flip image if necessary
        if (moveCmp.xDirection != 0f) {
            animCmp.flipImage = moveCmp.xDirection < 0
        }
    }

    private fun updateAnimation(animCmp: AnimationComponent, newAnimation: AnimationType) {
        if (currentAnimation != newAnimation) {
            animCmp.nextAnimation(newAnimation)
            currentAnimation = newAnimation
        }
    }

    companion object {
        private val LOG = logger<MoveSystem>()
    }
}
