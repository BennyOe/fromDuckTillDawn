package io.bennyoe

object GameConstants {
    // World constants
    const val UNIT_SCALE = 1 / 16f
    const val PHYSIC_TIME_STEP = 1 / 45f
    const val GAME_WIDTH = 1280f
    const val GAME_HEIGHT = 1024f
    const val WORLD_WIDTH = 16f
    const val WORLD_HEIGHT = 9f

    // Physic constants
    const val GRAVITY = -50.81f
    const val JUMP_CUT_FACTOR = .2f
    const val FALL_GRAVITY_SCALE = 2f

    // Player constants
    const val DOUBLE_JUMP_GRACE_TIME = 0.2f
    const val JUMP_BUFFER = .2f
    const val JUMP_MAX_HEIGHT = 3f
    const val WALK_MAX_SPEED = 5f
    const val BASH_COOLDOWN = .2f
    const val BASH_POWER = 5f

    // scaling constant because the temp player model doesn't fit the 16x16 world with its 64x100
    const val PLAYER_SCALING_X = .2f
    const val PLAYER_SCALING_Y = .5f

    // Camera constants
    const val CAMERA_SMOOTHING_FACTOR = 0.04f
}
