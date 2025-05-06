package io.bennyoe.systems

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import io.bennyoe.components.AnimationComponent
import io.bennyoe.components.InputComponent
import io.bennyoe.components.MoveComponent
import io.bennyoe.components.PhysicComponent
import io.bennyoe.components.WalkDirection
import ktx.log.logger

class MoveSystem : IteratingSystem(family { all(PhysicComponent, MoveComponent, AnimationComponent, InputComponent) }, enabled = true) {
    override fun onTickEntity(entity: Entity) {
        val moveCmp = entity[MoveComponent]
        val inputCmp = entity[InputComponent]
        val animationCmp = entity[AnimationComponent]
        when (inputCmp.direction) {
            WalkDirection.NONE -> moveCmp.moveVelocity = 0f
            WalkDirection.LEFT -> {
                animationCmp.flipImage = true
                moveCmp.moveVelocity = -moveCmp.maxSpeed
            }

            WalkDirection.RIGHT -> {
                animationCmp.flipImage = false
                moveCmp.moveVelocity = moveCmp.maxSpeed
            }
        }
    }

    companion object {
        val logger = logger<MoveSystem>()
    }
}
