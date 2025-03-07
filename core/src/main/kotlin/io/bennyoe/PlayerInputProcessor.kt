package io.bennyoe

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Keys
import com.github.quillraven.fleks.World
import io.bennyoe.components.MoveComponent
import io.bennyoe.components.PlayerComponent
import ktx.app.KtxInputAdapter
import ktx.log.logger

class PlayerInputProcessor(
    world: World
) : KtxInputAdapter {

    private val playerEntities = world.family { all(PlayerComponent) }

    // Mapping der Steuerungstasten zu Aktionen
    private val keyActions = mapOf(
        Keys.W to Action.JUMP,
        Keys.A to Action.MOVE_LEFT,
        Keys.D to Action.MOVE_RIGHT,
        Keys.SPACE to Action.ATTACK
    )

    init {
        Gdx.input.inputProcessor = this
    }

    override fun keyDown(keycode: Int): Boolean {
        keyActions[keycode]?.let { handleAction(it, true) } ?: return false
        return true
    }

    override fun keyUp(keycode: Int): Boolean {
        keyActions[keycode]?.let { handleAction(it, false) } ?: return false
        return true
    }

    private fun handleAction(action: Action, active: Boolean) {
        playerEntities.forEach { playerEntity ->
            val moveComponent = playerEntity[MoveComponent]

            when (action) {
                Action.JUMP -> moveComponent.yMovement = if (active) 1f else 0f
                Action.MOVE_LEFT -> moveComponent.xMovement = if (active) -1f else 0f
                Action.MOVE_RIGHT -> moveComponent.xMovement = if (active) 1f else 0f
                Action.ATTACK -> moveComponent.attack = active
            }
        }
    }

    companion object {
        val LOG = logger<PlayerInputProcessor>()
    }

    enum class Action {
        JUMP, MOVE_LEFT, MOVE_RIGHT, ATTACK
    }
}
