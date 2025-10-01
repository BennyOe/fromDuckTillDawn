package io.bennyoe.components

import com.badlogic.gdx.math.Vector2
import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import io.bennyoe.config.GameConstants.SWIM_MAX_SPEED
import io.bennyoe.config.GameConstants.WALK_MAX_SPEED
import ktx.math.vec2

data class MoveComponent(
    var maxWalkSpeed: Float = WALK_MAX_SPEED,
    var maxSwimSpeed: Float = SWIM_MAX_SPEED,
    var chaseSpeed: Float = 0f,
    var moveVelocity: Vector2 = vec2(),
    var lockMovement: Boolean = false,
    var throwBack: Boolean = false,
    var throwBackCooldown: Float = 0f,
) : Component<MoveComponent> {
    override fun type() = MoveComponent

    companion object : ComponentType<MoveComponent>() {}
}

enum class WalkDirection {
    NONE,
    LEFT,
    RIGHT,
}
