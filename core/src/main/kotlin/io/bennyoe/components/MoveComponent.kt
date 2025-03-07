package io.bennyoe.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

data class MoveComponent(
    var speed: Float = 5f,
    var jumpBoost: Float = 2.5f,
    var xMovement: Float = 0f,
    var yMovement: Float = 0f,
    var attack: Boolean = false
) : Component<MoveComponent> {
    override fun type() = MoveComponent

    companion object : ComponentType<MoveComponent>()
}
