package io.bennyoe.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

class JumpComponent(
    var jumpVelocity: Float = 0f,
    var maxHeight: Float = 3f,
) : Component<JumpComponent> {
    override fun type() = JumpComponent

    companion object : ComponentType<JumpComponent>()
}
