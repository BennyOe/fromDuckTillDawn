package io.bennyoe.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

class JumpComponent(
    var jumpVelocity: Float = 0f,
    var maxHeight: Float = 3f,
    var doubleJumpGraceTimer: Float = DOUBLE_JUMP_GRACE_TIME,
) : Component<JumpComponent> {
    override fun type() = JumpComponent

    fun resetDoubleJumpGraceTimer() {
        doubleJumpGraceTimer = DOUBLE_JUMP_GRACE_TIME
    }

    companion object : ComponentType<JumpComponent>() {
        const val DOUBLE_JUMP_GRACE_TIME = 0.3f
    }
}
