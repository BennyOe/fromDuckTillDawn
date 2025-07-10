package io.bennyoe.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import io.bennyoe.config.GameConstants.WALK_MAX_SPEED

data class MoveComponent(
    var maxSpeed: Float = WALK_MAX_SPEED,
    var chaseSpeed: Float = 0f,
    var moveVelocity: Float = 0f,
    var lockMovement: Boolean = false,
    var throwBack: Boolean = false,
) : Component<MoveComponent> {
    override fun type() = MoveComponent

    companion object : ComponentType<MoveComponent>() {}
}

enum class WalkDirection {
    NONE,
    LEFT,
    RIGHT,
}
