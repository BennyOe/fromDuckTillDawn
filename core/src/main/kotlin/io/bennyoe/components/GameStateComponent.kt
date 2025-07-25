package io.bennyoe.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import io.bennyoe.components.debug.DebugComponent.Companion.logger

class GameStateComponent(
    var isPaused: Boolean = false,
    var isLightingEnabled: Boolean = true,
    var gameMood: GameMood = GameMood.NORMAL,
) : Component<GameStateComponent> {
    private var alreadyChanged: Boolean = false

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
