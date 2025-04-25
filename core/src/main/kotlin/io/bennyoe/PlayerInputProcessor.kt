package io.bennyoe

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.ai.msg.MessageManager
import com.github.quillraven.fleks.World
import io.bennyoe.ai.FsmMessageTypes
import io.bennyoe.components.InputComponent
import io.bennyoe.components.WalkDirection
import ktx.app.KtxInputAdapter
import ktx.log.logger

class PlayerInputProcessor(
    world: World,
) : KtxInputAdapter {
    private val inputEntities = world.family { all(InputComponent) }
    private val messageDispatcher = MessageManager.getInstance()

    // Mapping der Steuerungstasten zu Aktionen
    private val keyActions =
        mapOf(
            Keys.W to Action.JUMP,
            Keys.A to Action.MOVE_LEFT,
            Keys.D to Action.MOVE_RIGHT,
            Keys.S to Action.CROUCH,
            Keys.SPACE to Action.ATTACK,
            Keys.J to Action.BASH,
            Keys.V to Action.MESSAGE,
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

    private fun handleAction(
        action: Action,
        pressed: Boolean,
    ) {
        inputEntities.forEach { input ->
            val inputComponent = input[InputComponent]
            when (action) {
                Action.JUMP -> inputComponent.jumpJustPressed = pressed
                Action.CROUCH -> inputComponent.crouch = pressed
                Action.ATTACK -> inputComponent.attackJustPressed = pressed
                Action.BASH -> inputComponent.bashJustPressed = pressed
                Action.MOVE_LEFT -> inputComponent.direction = if (pressed) WalkDirection.LEFT else WalkDirection.NONE
                Action.MOVE_RIGHT -> inputComponent.direction = if (pressed) WalkDirection.RIGHT else WalkDirection.NONE
                Action.MESSAGE ->
                    messageDispatcher.dispatchMessage(
                        0f,
                        FsmMessageTypes.HEAL.ordinal,
                        pressed,
                    )
            }
        }
    }

    companion object {
        val logger = logger<PlayerInputProcessor>()
    }

    enum class Action {
        JUMP,
        MOVE_LEFT,
        MOVE_RIGHT,
        ATTACK,
        BASH,
        CROUCH,
        MESSAGE,
    }
}
