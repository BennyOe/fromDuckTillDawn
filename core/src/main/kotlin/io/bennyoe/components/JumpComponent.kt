package io.bennyoe.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

class JumpComponent(
    var jumpVelocity: Float = 0f,
    var maxHeight: Float = 2f,
    var jumpCounter: Int = 0,
    var timeSinceGrounded: Float = 0f,
    val maxCoyoteTime: Float = .1f,
) : Component<JumpComponent> {
    override fun type() = JumpComponent

    companion object : ComponentType<JumpComponent>()
}
