package io.bennyoe.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

data class TimeScaleComponent(
    var current: Float = 1f,
    var hitStopTimer: Float = 0f,
) : Component<TimeScaleComponent> {
    override fun type() = TimeScaleComponent

    companion object : ComponentType<TimeScaleComponent>()
}
