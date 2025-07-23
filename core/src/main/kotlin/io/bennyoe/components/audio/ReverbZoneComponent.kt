package io.bennyoe.components.audio

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

class ReverbZoneComponent(
    val presetName: String,
    val intensity: Float = 0.5f,
) : Component<ReverbZoneComponent> {
    override fun type() = ReverbZoneComponent

    companion object : ComponentType<ReverbZoneComponent>()
}
