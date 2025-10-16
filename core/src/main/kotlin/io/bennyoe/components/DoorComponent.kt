package io.bennyoe.components

import com.badlogic.gdx.math.Vector2
import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

class DoorComponent(
    val id: String,
    var isOpen: Boolean = false,
    var initialPosition: Vector2? = null,
    var targetY: Float? = null,
) : Component<DoorComponent> {
    override fun type() = DoorComponent

    companion object : ComponentType<DoorComponent>()
}
