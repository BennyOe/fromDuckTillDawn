package io.bennyoe.components.ai

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

class HearingComponent(
    val hearingRadius: Float,
) : Component<HearingComponent> {
    override fun type() = HearingComponent

    companion object : ComponentType<HearingComponent>()
}
