package io.bennyoe.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

data class MoveComponent(
    var acceleration: Float = 0.9f,
    var deceleration: Float = .1f,
    var speed: Float = 7f,
    var jumpBoost: Float = 17.5f,
    var jumpCounter: Int = 0,
    var xDirection: Float = 0f,
    var yDirection: Float = 1f,
    var jumpRequest: Boolean = false,
    var timeSinceGrounded: Float = 0f,
    val maxCoyoteTime: Float = .1f,
    var crouchMode: Boolean = false,
    var walking: Boolean = false
) : Component<MoveComponent> {


    override fun type() = MoveComponent

    companion object : ComponentType<MoveComponent>() {}
}
