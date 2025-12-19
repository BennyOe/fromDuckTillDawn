package io.bennyoe.components.ai

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

class FieldOfViewResultComponent : Component<FieldOfViewResultComponent> {
    var raysHitting: Int = 0
    var distanceToPlayer: Float = 0f
    var illuminationOfPlayer: Double = 0.0

    override fun type() = FieldOfViewResultComponent

    companion object : ComponentType<FieldOfViewResultComponent>()
}
