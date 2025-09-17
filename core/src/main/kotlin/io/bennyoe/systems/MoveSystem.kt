package io.bennyoe.systems

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import io.bennyoe.components.AnimationComponent
import io.bennyoe.components.ImageComponent
import io.bennyoe.components.IntentionComponent
import io.bennyoe.components.MoveComponent
import io.bennyoe.components.PhysicComponent
import io.bennyoe.components.StateComponent
import io.bennyoe.components.WalkDirection
import io.bennyoe.state.player.PlayerFSM
import ktx.log.logger

class MoveSystem :
    IteratingSystem(family { all(PhysicComponent, MoveComponent, AnimationComponent) }, enabled = true),
    PausableSystem {
    override fun onTickEntity(entity: Entity) {
        val moveCmp = entity[MoveComponent]
        val intentionCmp = entity[IntentionComponent]
        val imageCmp = entity[ImageComponent]
        val stateCmp = entity[StateComponent]
        val physicCmp = entity[PhysicComponent]

        if (moveCmp.lockMovement) {
            return
        }

        if (stateCmp.stateMachine.currentState == PlayerFSM.SWIM) {
            when (intentionCmp.walkDirection) {
                WalkDirection.NONE -> moveCmp.moveVelocity.x = 0f
                WalkDirection.LEFT -> {
                    imageCmp.flipImage = true
                    moveCmp.moveVelocity.x = -moveCmp.maxSwimSpeed
                }

                WalkDirection.RIGHT -> {
                    imageCmp.flipImage = false
                    moveCmp.moveVelocity.x = moveCmp.maxSwimSpeed
                }
            }

            if (intentionCmp.wantsToSwimUp && physicCmp.isUnderWater) {
                moveCmp.moveVelocity.y = moveCmp.maxSwimSpeed
            } else if (intentionCmp.wantsToSwimDown) {
                moveCmp.moveVelocity.y = -moveCmp.maxSwimSpeed
            } else {
                moveCmp.moveVelocity.y = 0f
            }
        } else {
            when (intentionCmp.walkDirection) {
                WalkDirection.NONE -> moveCmp.moveVelocity.x = 0f
                WalkDirection.LEFT -> {
                    imageCmp.flipImage = true
                    if (intentionCmp.wantsToChase) {
                        moveCmp.moveVelocity.x = -moveCmp.chaseSpeed
                    } else {
                        moveCmp.moveVelocity.x = -moveCmp.maxWalkSpeed
                    }
                }

                WalkDirection.RIGHT -> {
                    imageCmp.flipImage = false
                    if (intentionCmp.wantsToChase) {
                        moveCmp.moveVelocity.x = moveCmp.chaseSpeed
                    } else {
                        moveCmp.moveVelocity.x = moveCmp.maxWalkSpeed
                    }
                }
            }
        }
    }

    companion object {
        val logger = logger<MoveSystem>()
    }
}
