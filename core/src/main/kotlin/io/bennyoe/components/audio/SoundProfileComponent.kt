package io.bennyoe.components.audio

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import io.bennyoe.utility.SoundProfile

class SoundProfileComponent(
    val profile: SoundProfile = SoundProfile(),
) : Component<SoundProfileComponent> {
    override fun type() = SoundProfileComponent

    companion object : ComponentType<SoundProfileComponent>()
}
