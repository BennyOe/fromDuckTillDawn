package io.bennyoe.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

class IntentionComponent(
    // Shared intentions
    var walkDirection: WalkDirection = WalkDirection.NONE,
    var wantsToJump: Boolean = false,
    var wantsToAttack: Boolean = false,
    // Player intentions
    var wantsToAttack2: Boolean = false,
    var wantsToAttack3: Boolean = false,
    var wantsToCrouch: Boolean = false,
    var wantsToBash: Boolean = false,
    var wantsToSwimUp: Boolean = false,
    var wantsToSwimDown: Boolean = false,
    // Mushroom intentions
    var wantsToChase: Boolean = false,
    // Minotaur intentions
    var wantsToScream: Boolean = false,
    var wantsToGrabAttack: Boolean = false,
    var wantsToThrowAttack: Boolean = false,
    var wantsToStomp: Boolean = false,
) : Component<IntentionComponent> {
    override fun type() = IntentionComponent

    fun resetAllIntentions() {
        walkDirection = WalkDirection.NONE
        wantsToJump = false
        wantsToAttack = false
        wantsToAttack2 = false
        wantsToAttack3 = false
        wantsToCrouch = false
        wantsToBash = false
        wantsToSwimUp = false
        wantsToSwimDown = false
        wantsToChase = false
        wantsToScream = false
        wantsToGrabAttack = false
        wantsToThrowAttack = false
        wantsToStomp = false
    }

    companion object : ComponentType<IntentionComponent>()
}
