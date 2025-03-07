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
            moveCmp.yMovement > 0 -> AnimationType.JUMP
            moveCmp.attack -> AnimationType.ATTACK
            moveCmp.xMovement != 0f -> AnimationType.WALK
            else -> AnimationType.IDLE
        }

        // only update when animation changed
        updateAnimation(animCmp, newAnimation)

        // set impulses
        var impulseY = 0f
        if (entity has HasGroundContact) {
            impulseY = if (moveCmp.yMovement > 0) mass * moveCmp.jumpBoost * moveCmp.yMovement + velY else 0f
        }
        val impulseX = if (moveCmp.xMovement != 0f) mass * moveCmp.speed * moveCmp.xMovement - velX else -mass * velX
        phyCmp.impulse = Vector2(impulseX, impulseY)

        // flip image if necessary
        if (moveCmp.xMovement != 0f) {
            animCmp.flipImage = moveCmp.xMovement < 0
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
