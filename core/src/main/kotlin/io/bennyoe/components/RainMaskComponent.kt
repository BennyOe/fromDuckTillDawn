package io.bennyoe.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

class RainMaskComponent(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
) : Component<RainMaskComponent> {
    override fun type() = RainMaskComponent

    companion object : ComponentType<RainMaskComponent>()
}
