package io.bennyoe.components

import com.badlogic.gdx.math.Vector2
import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

class ParallaxComponent(
    val factor: Vector2,
    val initialPosition: Vector2,
    val worldWidth: Float,
    val worldHeight: Float,
) : Component<ParallaxComponent> {
    override fun type() = ParallaxComponent

    companion object : ComponentType<ParallaxComponent>()
}
