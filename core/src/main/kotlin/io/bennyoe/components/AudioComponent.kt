package io.bennyoe.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import de.pottgames.tuningfork.BufferedSoundSource
import io.bennyoe.assets.SoundAssets
import io.bennyoe.systems.SoundTypes
import ktx.log.logger

class AudioComponent(
    val soundAsset: SoundAssets,
    val soundVolume: Float,
    val soundAttenuationMaxDistance: Float,
    val soundAttenuationMinDistance: Float,
    val soundAttenuationFactor: Float,
    val isLooping: Boolean = true,
    val soundTypes: SoundTypes = SoundTypes.NONE,
) : Component<AudioComponent> {
    var bufferedSoundSource: BufferedSoundSource? = null

    override fun type() = AudioComponent

    override fun World.onRemove(entity: Entity) {
        bufferedSoundSource?.stop()
    }

    companion object : ComponentType<AudioComponent>() {
        val logger = logger<AudioComponent>()
    }
}
