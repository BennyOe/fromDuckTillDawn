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

    // Player constats
    const val DOUBLE_JUMP_GRACE_TIME = 0.2f
    const val JUMP_BUFFER = .2f
    const val JUMP_MAX_HEIGHT = 3f
    const val WALK_MAX_SPEED = 5f
    const val BASH_COOLDOWN = .2f
    const val BASH_POWER = 5f
}
