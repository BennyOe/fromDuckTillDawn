package io.bennyoe.utility

import io.bennyoe.assets.SoundAssets
import io.bennyoe.service.SoundType

data class SoundProfile(
    // non contextual sounds
    val simpleSounds: Map<SoundType, List<SoundAssets>> = emptyMap(),
    // footsteps sounds depends on the surface
    val footstepsSounds: Map<FloorType, List<SoundAssets>> = emptyMap(),
)
