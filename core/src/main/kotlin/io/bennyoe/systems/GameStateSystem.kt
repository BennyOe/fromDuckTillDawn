package io.bennyoe.systems

import com.github.quillraven.fleks.IntervalSystem
import io.bennyoe.components.GameStateComponent
import ktx.log.logger

class GameStateSystem : IntervalSystem() {
    private var alReadyChanged = false

    override fun onTick() {
        val gameStateEntity = world.family { all(GameStateComponent) }.firstOrNull() ?: return
        val gameStateCmp = gameStateEntity[GameStateComponent]

        // Prevents redundant state changes: only proceed if the pause state has actually changed
        if (gameStateCmp.isPaused == alReadyChanged) return
        alReadyChanged = gameStateCmp.isPaused

        world.systems.filter { it is PausableSystem }.forEach { it.enabled = !gameStateCmp.isPaused }
    }

    companion object {
        val logger = logger<GameStateSystem>()
    }
}
