package io.bennyoe.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

class IntentionComponent(
    var walkDirection: WalkDirection = WalkDirection.NONE,
    var wantsToJump: Boolean = false,
    var wantsToAttack: Boolean = false,
    var wantsToAttack2: Boolean = false,
    var wantsToAttack3: Boolean = false,
    var wantsToCrouch: Boolean = false,
    var wantsToBash: Boolean = false,
    var wantsToChase: Boolean = false,
    var wantsToSwimUp: Boolean = false,
    var wantsToSwimDown: Boolean = false,
) : Component<IntentionComponent> {
    override fun type() = IntentionComponent

    companion object : ComponentType<IntentionComponent>()
}
