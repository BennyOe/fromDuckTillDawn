package io.bennyoe.config

object GameConstants {
    // ------------------------- World constants ------------------------------//
    const val TIME_SCALE = 1f // Game speed from 0% to 100%
    const val UNIT_SCALE = 1 / 16f // Scale from pixel to world units
    const val PHYSIC_TIME_STEP = 1 / 45f // update per second of the physics engine
    const val GAME_WIDTH = 1280f
    const val GAME_HEIGHT = 1024f
    const val WORLD_WIDTH = 16f
    const val WORLD_HEIGHT = 9f

    // ------------------------- Physic constants ------------------------------//
    const val GRAVITY = -50.81f
    const val JUMP_CUT_FACTOR = .2f // cuts the vertical impulse when releasing the jump button
    const val FALL_GRAVITY_SCALE = 2f // scales the gravity when falling

    // ------------------------- Player constants ------------------------------//
    const val DOUBLE_JUMP_GRACE_TIME = 0.2f // time you can be falling and still perform double jump
    const val JUMP_BUFFER = .2f // time you can press jump before hitting the ground and still jump
    const val JUMP_MAX_HEIGHT = 5f
    const val WALK_MAX_SPEED = 5f
    const val BASH_COOLDOWN = .2f
    const val BASH_POWER = 5f

    // ------------------------- Mushroom constants ------------------------------//
    const val NORMAL_DETECTION_RADIUS = 4f
    const val CHASE_DETECTION_RADIUS = 7f
    const val CHASE_SPEED = 4f

    // ------------------------- Camera constants ------------------------------//
    const val CAMERA_SMOOTHING_FACTOR = 0.04f // smoothes the camera movement, so that it is delayed of the player movement
    const val CAMERA_ZOOM_FACTOR = 2.3f

    // ------------------------- Debug constants ------------------------------//
    const val ENABLE_DEBUG = true // enables the debug draw
    const val SHOW_ATTACK_DEBUG = true
    const val SHOW_PLAYER_DEBUG = true
    const val SHOW_CAMERA_DEBUG = false
    const val SHOW_ENEMY_DEBUG = true
    const val DEBUG_ALPHA = 0.5f
    const val SHOW_ONLY_DEBUG = false // disables the rendering of the game and only shows the shapes

    // ------------------------- Sound constants ------------------------------//
    const val MUSIC_VOLUME = 0f
    const val AMBIENCE_VOLUME = 1f
    const val EFFECT_VOLUME = 1f

    // ------------------------- Game constants ------------------------------//
    const val DYNAMIC_TIME_OF_DAY = false
    const val TIME_OF_DAY_SPEED = 2.6f
    const val INITIAL_TIME_OF_DAY = 3f
    const val RAIN_DELAY = 22f
    const val HIT_STOP_DURATION = 0.2f
}
