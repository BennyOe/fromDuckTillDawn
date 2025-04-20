package io.bennyoe.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

data class MoveComponent(
    var maxSpeed: Float = 5f,
    var moveVelocity: Float = 0f
) : Component<MoveComponent> {


    override fun type() = MoveComponent

    companion object : ComponentType<MoveComponent>() {}
}
