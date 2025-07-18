package io.bennyoe.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

class AudioZoneComponent(
    val effect: String,
    val preset: String? = null,
    val intensity: Float? = null,
) : Component<AudioZoneComponent> {
    override fun type() = AudioZoneComponent

    companion object : ComponentType<AudioZoneComponent>()
}
