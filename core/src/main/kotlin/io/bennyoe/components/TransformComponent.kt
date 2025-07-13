package io.bennyoe.components

import com.badlogic.gdx.math.Vector2
import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

class TransformComponent(
    var position: Vector2,
    var width: Float,
    var height: Float,
) : Component<TransformComponent> {
    override fun type() = TransformComponent

    companion object : ComponentType<TransformComponent>()
}
