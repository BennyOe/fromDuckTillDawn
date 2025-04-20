package io.bennyoe.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

data class InputComponent(
    var attackJustPressed: Boolean = false,
    var jumpJustPressed: Boolean = false,
    var bashJustPressed: Boolean = false,
    var crouch: Boolean = false,
    var direction: WalkDirection = WalkDirection.NONE,
) : Component<InputComponent> {
    override fun type() = InputComponent

    companion object : ComponentType<InputComponent>()
}

enum class WalkDirection{
    NONE, LEFT, RIGHT
}
