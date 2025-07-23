package io.bennyoe.utility

import de.pottgames.tuningfork.EaxReverb
import de.pottgames.tuningfork.SoundEffect
import io.bennyoe.systems.AudioSystem
import ktx.log.logger

fun getReverb(presetName: String): SoundEffect? {
    val soundEffect =
        when (presetName.uppercase()) {
            "CAVE" -> SoundEffect(EaxReverb.cave())
            "FOREST" -> SoundEffect(EaxReverb.forest())
            "ARENA" -> SoundEffect(EaxReverb.arena())
            "HANGER" -> SoundEffect(EaxReverb.hangar())
            "STONEROOM" -> SoundEffect(EaxReverb.stoneRoom())
            else -> {
                logger<AudioSystem>().error { "Unknown EaxReverb preset: '$presetName'" }
                return null
            }
        }
    logger<AudioSystem>().debug { "Applied EaxReverb preset: $presetName" }
    return soundEffect
}
