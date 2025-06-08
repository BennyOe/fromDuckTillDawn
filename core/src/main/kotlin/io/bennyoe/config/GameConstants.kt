package io.bennyoe.config

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

    // Camera constants
    const val CAMERA_SMOOTHING_FACTOR = 0.04f

    // Debug constants
    const val ENABLE_DEBUG = true
    const val SHOW_ATTACK_DEBUG = true
    const val SHOW_PLAYER_DEBUG = true
    const val SHOW_CAMERA_DEBUG = false
    const val SHOW_ENEMY_DEBUG = true
    const val DEBUG_ALPHA = 0.5f

    // Show only debug without rendering images
    const val SHOW_ONLY_DEBUG = false
}
