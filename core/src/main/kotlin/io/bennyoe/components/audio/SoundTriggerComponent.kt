package io.bennyoe.components.audio

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import io.bennyoe.systems.audio.SoundType

class SoundTriggerComponent(
    val sound: String?,
    val type: SoundType?,
    val streamed: Boolean,
    val volume: Float = 0.5f,
) : Component<SoundTriggerComponent> {
    override fun type() = SoundTriggerComponent

    companion object : ComponentType<SoundTriggerComponent>()
}
