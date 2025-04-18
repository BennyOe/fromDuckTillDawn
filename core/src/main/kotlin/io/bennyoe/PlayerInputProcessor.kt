package io.bennyoe

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Keys
import com.github.quillraven.fleks.World
import io.bennyoe.components.InputComponent
import ktx.app.KtxInputAdapter
import ktx.log.logger

class PlayerInputProcessor(
    world: World
) : KtxInputAdapter {

    private val inputEntities = world.family { all(InputComponent) }

    // Mapping der Steuerungstasten zu Aktionen
    private val keyActions = mapOf(
        Keys.W to Action.JUMP,
        Keys.A to Action.MOVE_LEFT,
        Keys.D to Action.MOVE_RIGHT,
        Keys.S to Action.CROUCH,
        Keys.SPACE to Action.ATTACK,
        Keys.J to Action.BASH
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

    private fun handleAction(action: Action, pressed: Boolean) {
        inputEntities.forEach { input ->
            val inputComponent = input[InputComponent]
            when (action) {
                Action.JUMP -> inputComponent.jump = pressed
                Action.CROUCH -> inputComponent.crouch = pressed
                Action.ATTACK -> inputComponent.attack = pressed
                Action.BASH -> inputComponent.bash = pressed
                Action.MOVE_LEFT -> inputComponent.xDirection = if (pressed) -1f else 0f
                Action.MOVE_RIGHT -> inputComponent.xDirection = if (pressed) 1f else 0f
            }
        }
    }

    companion object {
        val logger = logger<PlayerInputProcessor>()
    }

    enum class Action {
        JUMP, MOVE_LEFT, MOVE_RIGHT, ATTACK, BASH, CROUCH
    }
}
