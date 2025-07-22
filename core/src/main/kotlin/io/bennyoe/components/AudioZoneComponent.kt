package io.bennyoe.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import io.bennyoe.utility.SoundEffectEnum

class AudioZoneComponent(
    val effect: SoundEffectEnum,
    val presetName: String? = null,
    val intensity: Float? = null,
    val fadeInDuration: Float = 0f,
    val fadeOutDuration: Float = 0f,
) : Component<AudioZoneComponent> {
    override fun type() = AudioZoneComponent

    companion object : ComponentType<AudioZoneComponent>()
}
