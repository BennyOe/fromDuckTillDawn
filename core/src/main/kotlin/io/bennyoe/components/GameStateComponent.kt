package io.bennyoe.components

import com.badlogic.gdx.graphics.Color
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
    var isTriggerWeatherJustPressed: Boolean = false,
    var weather: Weather = Weather.CLEAR,
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

    fun toggleWeatherChange(pressed: Boolean) {
        if (pressed && !alreadyChanged) {
            isTriggerWeatherJustPressed = !isTriggerWeatherJustPressed
            alreadyChanged = true
            val values = Weather.entries.toTypedArray()
            val nextIndex = (weather.ordinal + 1) % values.size
            weather = values[nextIndex]
            logger.debug { "Weather change is triggered to ${weather.name}" }
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

enum class Weather(
    val spawnSpeed: Float,
    val minZIndex: Int = 1000,
    val maxZIndex: Int = 1000,
    val minSize: Float = 16f,
    val maxSize: Float = 32f,
    val minImageAlpha: Float = .7f,
    val minHeightMultiplier: Float = .8f,
    val shadowMultiplier: Float = 1f,
    val lightMultiplier: Color = Color(1f, 1f, 1f, 1f),
    val transitionDuration: Float = 8f,
) {
    CLEAR(spawnSpeed = -1f),
    PARTIALLY_CLOUDY(spawnSpeed = 20f),
    CLOUDY(
        spawnSpeed = 6f,
        maxZIndex = 3000,
        shadowMultiplier = .8f,
        lightMultiplier = Color(.8f, .8f, .8f, .8f),
    ),
    RAIN(
        spawnSpeed = 1f,
        maxZIndex = 4000,
        minSize = 32f,
        maxSize = 64f,
        minImageAlpha = 1f,
        minHeightMultiplier = .5f,
        shadowMultiplier = .2f,
        lightMultiplier = Color(.8f, .8f, .8f, .8f),
        transitionDuration = 20f,
    ),
}
