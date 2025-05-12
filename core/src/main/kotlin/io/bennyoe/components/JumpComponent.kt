package io.bennyoe.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import io.bennyoe.GameConstants.DOUBLE_JUMP_GRACE_TIME
import io.bennyoe.GameConstants.JUMP_BUFFER
import io.bennyoe.GameConstants.JUMP_MAX_HEIGHT

class JumpComponent(
    var wantsToJump: Boolean = false,
    var jumpVelocity: Float = 0f,
    var maxHeight: Float = JUMP_MAX_HEIGHT,
    var doubleJumpGraceTimer: Float = DOUBLE_JUMP_GRACE_TIME,
    var jumpBuffer: Float = -1f,
) : Component<JumpComponent> {
    override fun type() = JumpComponent

    fun resetJumpBuffer() {
        jumpBuffer = JUMP_BUFFER
    }

    fun disableJumpBuffer() {
        jumpBuffer = -1f
    }

    fun resetDoubleJumpGraceTimer() {
        doubleJumpGraceTimer = DOUBLE_JUMP_GRACE_TIME
    }

    fun disableDoubleJumpGraceTimer() {
        doubleJumpGraceTimer = -1f
    }

    companion object : ComponentType<JumpComponent>()
}
