package io.bennyoe.components

import com.badlogic.gdx.math.Vector2
import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

class TransformComponent(
    var position: Vector2,
    var width: Float,
    var height: Float,
) : Component<TransformComponent> {
    val bottom: Float
        get() = position.y - height * 0.5f
    val top: Float
        get() = position.y + height * 0.5f
    val left: Float
        get() = position.x - width * 0.5f
    val right: Float
        get() = position.x + width * 0.5f

    override fun type() = TransformComponent

    companion object : ComponentType<TransformComponent>()
}
