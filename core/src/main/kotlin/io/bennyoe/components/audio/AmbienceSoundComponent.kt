package io.bennyoe.components.audio

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import io.bennyoe.systems.audio.AmbienceType

class AmbienceSoundComponent(
    val type: AmbienceType,
    val sound: String,
    val volume: Float? = 0.4f,
) : Component<AmbienceSoundComponent> {
    override fun type() = AmbienceSoundComponent

    companion object : ComponentType<AmbienceSoundComponent>()
}
