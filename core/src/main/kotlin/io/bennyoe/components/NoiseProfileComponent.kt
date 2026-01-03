package io.bennyoe.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import io.bennyoe.systems.audio.SoundType

class NoiseProfileComponent(
    // Global multiplier for all noises from this entity (e.g. 0.5 when crouching/sneaking)
    var noiseMultiplier: Float = 1.0f,
    // Default fallback if a sound is not explicitly mapped
    var defaultRange: Float = 0f,
    // Specific mapping for distinct actions
    val noises: Map<SoundType, NoiseSettings> = emptyMap(),
) : Component<NoiseProfileComponent> {
    override fun type() = NoiseProfileComponent

    companion object : ComponentType<NoiseProfileComponent>()
}

data class NoiseSettings(
    val range: Float,
    val type: NoiseType = NoiseType.SUSPICIOUS,
    val continuous: Boolean = false,
)

enum class NoiseType {
    NATURAL,
    SUSPICIOUS,
    ALARMING,
}
