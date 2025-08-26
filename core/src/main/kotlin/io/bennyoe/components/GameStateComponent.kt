package io.bennyoe.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import io.bennyoe.components.debug.DebugComponent.Companion.logger
import io.bennyoe.config.GameConstants.INITIAL_TIME_OF_DAY

class GameStateComponent(
    var isPaused: Boolean = false,
    var isLightingEnabled: Boolean = true,
    var gameMood: GameMood = GameMood.NORMAL,
    var isTriggerTimeOfDayJustPressed: Boolean = false,
    var timeOfDay: Float = INITIAL_TIME_OF_DAY,
) : Component<GameStateComponent> {
    private var alreadyChanged: Boolean = false

    fun getTimeOfDay(): TimeOfDay =
        if (timeOfDay in 6f..18f) {
            TimeOfDay.DAY
        } else {
            TimeOfDay.NIGHT
        }

    fun togglePause(pressed: Boolean) {
        if (pressed && !alreadyChanged) {
            isPaused = !isPaused
            alreadyChanged = true
            logger.debug { "GAME IS ${if (isPaused) "PAUSED" else "RESUMED"}" }
        }
        if (!pressed) {
            alreadyChanged = false
        }
    }

    fun toggleLighting(pressed: Boolean) {
        if (pressed && !alreadyChanged) {
            isLightingEnabled = !isLightingEnabled
            alreadyChanged = true
            logger.debug { "LIGHTING IS ${if (isLightingEnabled) "ENABLED" else "DISABLED"}" }
        }
        if (!pressed) {
            alreadyChanged = false
        }
    }

    fun toggleTimeOfDayChange(pressed: Boolean) {
        if (pressed && !alreadyChanged) {
            isTriggerTimeOfDayJustPressed = !isTriggerTimeOfDayJustPressed
            alreadyChanged = true
            logger.debug { "Day change is triggered" }
        }
        if (!pressed) {
            alreadyChanged = false
        }
    }

    override fun type() = GameStateComponent

    companion object : ComponentType<GameStateComponent>()
}

enum class GameMood(
    val priority: Int,
) {
    NORMAL(0),
    CHASE(1),
    PLAYER_DEAD(100),
}

enum class TimeOfDay { DAY, NIGHT, DAWN, DUSK, TWILIGHT }
