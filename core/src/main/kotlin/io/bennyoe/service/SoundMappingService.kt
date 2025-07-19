package io.bennyoe.service

import io.bennyoe.assets.SoundAssets
import io.bennyoe.utility.FloorType
import io.bennyoe.utility.SoundProfile

object SoundMappingService {
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
            // If the sound depends on the surface, look in the footstep map
            profile.footstepsSounds[floorType] ?: profile.footstepsSounds.values.first()
        } else {
            // Otherwise, look in the simple sounds map
            profile.simpleSounds[type]
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
