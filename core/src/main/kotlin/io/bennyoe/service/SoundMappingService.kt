package io.bennyoe.service

import io.bennyoe.assets.SoundAssets
import io.bennyoe.utility.FloorType
import io.bennyoe.utility.SoundProfile
import ktx.log.logger

object SoundMappingService {
    private val logger = logger<SoundMappingService>()
    private val defaultGeneralSounds: Map<SoundType, SoundAssets> =
        mapOf(
            SoundType.CAMPFIRE to SoundAssets.CAMPFIRE,
        )

    fun getSoundAsset(
        type: SoundType,
        profile: SoundProfile? = null,
        floorType: FloorType? = null,
    ): SoundAssets? {
        if (profile == null) {
            return defaultGeneralSounds[type]
        }

        return if (type.isSurfaceDependent) {
            profile.footstepsSounds[floorType]?.randomOrNull()
                ?: profile.footstepsSounds[FloorType.STONE]?.randomOrNull()
                ?: run {
                    logger.error { "No sound found for floorType '$floorType' and no default could be used." }
                    null
                }
        } else {
            profile.simpleSounds[type]?.randomOrNull() ?: run {
                logger.error { "No sound found for simple sound type '$type'." }
                null
            }
        }
    }
}

enum class SoundType(
    val vary: Boolean = false,
    val positional: Boolean = false,
    val isSurfaceDependent: Boolean = false,
) {
    NONE,

    // Dawn
    DAWN_FOOTSTEPS(true, false, true),
    DAWN_ATTACK_1(true),
    DAWN_ATTACK_2(true),
    DAWN_ATTACK_3(true),
    DAWN_HIT,
    DAWN_BASH(true),
    DAWN_DEATH,
    DAWN_JUMP(true),

    // Enemies
    FOOTSTEPS(true, true, true),
    ATTACK(true, true),
    HIT(false, true),

    // Environment
    CAMPFIRE,
}
