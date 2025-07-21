package io.bennyoe.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import de.pottgames.tuningfork.BufferedSoundSource
import io.bennyoe.service.SoundType
import ktx.log.logger

class AudioComponent(
    val soundType: SoundType,
    val soundVolume: Float,
    val soundAttenuationMaxDistance: Float,
    val soundAttenuationMinDistance: Float,
    val soundAttenuationFactor: Float,
    val isLooping: Boolean = true,
) : Component<AudioComponent> {
    var bufferedSoundSource: BufferedSoundSource? = null

    override fun type() = AudioComponent

    override fun World.onRemove(entity: Entity) {
        bufferedSoundSource?.stop()
        bufferedSoundSource?.free()
    }

    companion object : ComponentType<AudioComponent>() {
        val logger = logger<AudioComponent>()
    }
}
