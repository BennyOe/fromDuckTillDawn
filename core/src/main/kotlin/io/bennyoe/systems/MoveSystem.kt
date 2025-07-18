package io.bennyoe.systems

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import io.bennyoe.components.AnimationComponent
import io.bennyoe.components.ImageComponent
import io.bennyoe.components.IntentionComponent
import io.bennyoe.components.MoveComponent
import io.bennyoe.components.PhysicComponent
import io.bennyoe.components.WalkDirection
import ktx.log.logger

class MoveSystem :
    IteratingSystem(family { all(PhysicComponent, MoveComponent, AnimationComponent) }, enabled = true),
    PausableSystem {
    override fun onTickEntity(entity: Entity) {
        val moveCmp = entity[MoveComponent]
        val intentionCmp = entity[IntentionComponent]
        val imageCmp = entity[ImageComponent]

        if (moveCmp.lockMovement) {
            return
        }

        when (intentionCmp.walkDirection) {
            WalkDirection.NONE -> moveCmp.moveVelocity = 0f
            WalkDirection.LEFT -> {
                imageCmp.flipImage = true
                if (intentionCmp.wantsToChase) {
                    moveCmp.moveVelocity = -moveCmp.chaseSpeed
                } else {
                    moveCmp.moveVelocity = -moveCmp.maxSpeed
                }
            }

            WalkDirection.RIGHT -> {
                imageCmp.flipImage = false
                if (intentionCmp.wantsToChase) {
                    moveCmp.moveVelocity = moveCmp.chaseSpeed
                } else {
                    moveCmp.moveVelocity = moveCmp.maxSpeed
                }
            }
        }
    }

    companion object {
        val logger = logger<MoveSystem>()
    }
}
