package io.bennyoe.systems

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import io.bennyoe.components.AnimationComponent
import io.bennyoe.components.ImageComponent
import io.bennyoe.components.InputComponent
import io.bennyoe.components.MoveComponent
import io.bennyoe.components.PhysicComponent
import io.bennyoe.components.WalkDirection
import ktx.log.logger

class MoveSystem : IteratingSystem(family { all(PhysicComponent, MoveComponent, AnimationComponent) }, enabled = true) {
    override fun onTickEntity(entity: Entity) {
        val moveCmp = entity[MoveComponent]
        val inputCmp = entity.getOrNull(InputComponent)
        val imageCmp = entity[ImageComponent]

        if (moveCmp.lockMovement) {
            return
        }

        if (inputCmp != null) {
            with(inputCmp) {
                when {
                    walkLeftPressed -> moveCmp.walk = WalkDirection.LEFT
                    walkRightPressed -> moveCmp.walk = WalkDirection.RIGHT
                    else -> moveCmp.walk = WalkDirection.NONE
                }
            }
        }

        when (moveCmp.walk) {
            WalkDirection.NONE -> moveCmp.moveVelocity = 0f
            WalkDirection.LEFT -> {
                imageCmp.flipImage = true
                moveCmp.moveVelocity = -moveCmp.maxSpeed
            }

            WalkDirection.RIGHT -> {
                imageCmp.flipImage = false
                moveCmp.moveVelocity = moveCmp.maxSpeed
            }
        }
    }

    companion object {
        val logger = logger<MoveSystem>()
    }
}
