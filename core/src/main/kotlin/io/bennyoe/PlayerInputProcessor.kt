package io.bennyoe

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Keys
import com.github.quillraven.fleks.World
import io.bennyoe.components.AttackComponent
import io.bennyoe.components.MoveComponent
import io.bennyoe.components.PlayerComponent
import ktx.app.KtxInputAdapter
import ktx.log.logger

class PlayerInputProcessor(
    world: World
) : KtxInputAdapter {

    private val playerEntities = world.family { all(PlayerComponent) }
    private var isJumpKeyPressed = false

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
        playerEntities.forEach { playerEntity ->
            val moveComponent = playerEntity[MoveComponent]
            val attackComponent = playerEntity[AttackComponent]

            when (action) {
                Action.JUMP -> {
                    if (pressed && !isJumpKeyPressed) {
                        moveComponent.jumpRequest = true
                    }
                    isJumpKeyPressed = pressed
                }

                Action.CROUCH -> moveComponent.crouchMode = pressed
                Action.ATTACK -> attackComponent.attack = pressed
                Action.BASH -> attackComponent.bashRequest = pressed
                Action.MOVE_LEFT -> {
                    moveComponent.xDirection = if (pressed) -1f else 0f
                    moveComponent.walking = pressed
                }

                Action.MOVE_RIGHT -> {
                    moveComponent.xDirection = if (pressed) 1f else 0f
                    moveComponent.walking = pressed
                }
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
