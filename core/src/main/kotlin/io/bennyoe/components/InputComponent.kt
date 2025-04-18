package io.bennyoe.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

data class InputComponent(
    var attack: Boolean = false,
    var jump: Boolean = false,
    var xDirection: Float = 0f,
    var bash: Boolean = false,
    var crouch: Boolean = false,
) : Component<InputComponent> {
    override fun type() = InputComponent

    companion object : ComponentType<InputComponent>()
}
