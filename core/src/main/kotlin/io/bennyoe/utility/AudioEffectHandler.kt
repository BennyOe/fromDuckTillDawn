package io.bennyoe.utility

import de.pottgames.tuningfork.EaxReverb
import de.pottgames.tuningfork.SoundEffect
import ktx.log.logger

interface AudioEffectHandler {
    /**
     * Applies a preset to the given effect data.
     * @param effectData The sound effect data object to modify.
     * @param presetName The name of the preset to apply (e.g., "CAVE").
     * @return True if the preset was successfully applied, false otherwise.
     */
    fun applyPreset(
        effectData: SoundEffectEnum,
        presetName: String?,
    ): SoundEffect?
}

/**
 * A concrete handler for EaxReverb effects.
 */
object EaxReverbHandler : AudioEffectHandler {
    private val logger = logger<EaxReverbHandler>()

    override fun applyPreset(
        effectData: SoundEffectEnum,
        presetName: String?,
    ): SoundEffect? {
        if (effectData != SoundEffectEnum.EAX_REVERB) return null
        if (presetName == null) return null

        // The simple, safe, and performant when-statement is now encapsulated here.
        val soundEffect =
            when (presetName.uppercase()) {
                "CAVE" -> SoundEffect(EaxReverb.cave())
                "FOREST" -> SoundEffect(EaxReverb.forest())
                "ARENA" -> SoundEffect(EaxReverb.arena())
                "HANGER" -> SoundEffect(EaxReverb.hangar())
                "STONEROOM" -> SoundEffect(EaxReverb.stoneRoom())
                else -> {
                    logger.error { "Unknown EaxReverb preset: '$presetName'" }
                    return null
                }
            }
        logger.debug { "Applied EaxReverb preset: $presetName" }
        return soundEffect
    }
}

/**
 * The central registry that maps effect names from the editor to their handlers.
 */
object AudioEffectRegistry {
    private val handlers =
        mapOf<String, AudioEffectHandler>(
            // The key MUST match the string used in the Tiled editor for the "soundEffect" property.
            "EAX_REVERB" to EaxReverbHandler,
            // To support a new effect type like "Distortion", add its handler here:
            // "DISTORTION" to DistortionHandler
        )

    fun getHandler(effectName: SoundEffectEnum): AudioEffectHandler? = handlers[effectName.name]
}
