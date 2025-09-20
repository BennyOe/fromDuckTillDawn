package io.bennyoe.components.audio

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

class AmbienceSoundComponent(
    val type: AmbienceType,
    val variations: Map<SoundVariation, String>,
    val volume: Float? = 0.4f,
) : Component<AmbienceSoundComponent> {
    override fun type() = AmbienceSoundComponent

    companion object : ComponentType<AmbienceSoundComponent>()
}

enum class AmbienceType {
    NONE,
    FOREST,
    CAVE,
    UNDER_WATER,
}

enum class SoundVariation {
    BASE,
    DAY,
    NIGHT,
    RAIN,
}
