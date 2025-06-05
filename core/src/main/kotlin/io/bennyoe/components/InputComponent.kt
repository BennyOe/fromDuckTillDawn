package io.bennyoe.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

data class InputComponent(
    var attackJustPressed: Boolean = false,
    var attack2JustPressed: Boolean = false,
    var attack3JustPressed: Boolean = false,
    var jumpIsPressed: Boolean = false,
    var jumpJustPressed: Boolean = false,
    var bashJustPressed: Boolean = false,
    var crouchJustPressed: Boolean = false,
    var walkLeftJustPressed: Boolean = false,
    var walkRightJustPressed: Boolean = false,
) : Component<InputComponent> {
    override fun type() = InputComponent

    companion object : ComponentType<InputComponent>()
}
