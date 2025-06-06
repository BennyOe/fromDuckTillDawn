package io.bennyoe.systems

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import io.bennyoe.components.InputComponent
import io.bennyoe.components.IntentionComponent
import io.bennyoe.components.WalkDirection

class InputSystem : IteratingSystem(family { all(InputComponent, IntentionComponent) }) {
    override fun onTickEntity(entity: Entity) {
        val inputCmp = entity[InputComponent]
        val intentionCmp = entity[IntentionComponent]

        with(inputCmp) {
            when {
                walkLeftJustPressed -> intentionCmp.walkDirection = WalkDirection.LEFT
                walkRightJustPressed -> intentionCmp.walkDirection = WalkDirection.RIGHT
                else -> intentionCmp.walkDirection = WalkDirection.NONE
            }

            // ----- Events that gets held -----------
            intentionCmp.wantsToAttack2 = attack2JustPressed
            intentionCmp.wantsToAttack3 = attack3JustPressed
            intentionCmp.wantsToCrouch = crouchJustPressed

            // ----- Events that gets triggered only once -----------
            if (jumpJustPressed) {
                intentionCmp.wantsToJump = true
                jumpJustPressed = false
            }

            if (attackJustPressed) {
                intentionCmp.wantsToAttack = true
                attackJustPressed = false
            }

            if (bashJustPressed) {
                intentionCmp.wantsToBash = true
                bashJustPressed = false
            }
        }
    }
}
