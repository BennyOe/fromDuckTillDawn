package io.bennyoe.systems

import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.github.quillraven.fleks.IntervalSystem
import io.bennyoe.components.GameStateComponent
import io.bennyoe.event.AmbienceChangeEvent
import ktx.log.logger

class GameStateSystem :
    IntervalSystem(),
    EventListener {
    private var alReadyChanged = false

    override fun onTick() {
        val gameStateEntity = world.family { all(GameStateComponent) }.firstOrNull() ?: return
        val gameStateCmp = gameStateEntity[GameStateComponent]

        // Prevents redundant state changes: only proceed if the pause state has actually changed
        if (gameStateCmp.isPaused == alReadyChanged) return
        alReadyChanged = gameStateCmp.isPaused

        world.systems.filter { it is PausableSystem }.forEach { it.enabled = !gameStateCmp.isPaused }
    }

    override fun handle(event: Event): Boolean {
        val gameStateEntity = world.family { all(GameStateComponent) }.firstOrNull() ?: return false
        val gameStateCmp = gameStateEntity[GameStateComponent]
        when (event) {
            is AmbienceChangeEvent -> {
                gameStateCmp.playerIsIndoor = event.isIndoor
                return true
            }
        }
        return false
    }

    companion object {
        val logger = logger<GameStateSystem>()
    }
}
