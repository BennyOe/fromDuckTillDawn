package io.bennyoe

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.ai.msg.MessageManager
import com.github.quillraven.fleks.World
import io.bennyoe.components.InputComponent
import io.bennyoe.components.WalkDirection
import io.bennyoe.components.debug.DebugComponent
import io.bennyoe.state.FsmMessageTypes
import ktx.app.KtxInputAdapter
import ktx.log.logger

class PlayerInputProcessor(
    world: World,
) : KtxInputAdapter {
    private val inputEntities = world.family { all(InputComponent) }
    private val debugEntities = world.family { all(DebugComponent) }
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
            Keys.C to Action.MESSAGE2,
            Keys.BACKSPACE to Action.DEBUG,
            Keys.K to Action.KILL,
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
        debugEntities.forEach { debugEntity ->
            val debugComponent = debugEntity[DebugComponent]
            when (action) {
                Action.DEBUG -> debugComponent.toggleDebug(pressed)
                else -> Unit
            }
        }
        inputEntities.forEach { input ->
            val inputComponent = input[InputComponent]
            when (action) {
                Action.JUMP -> {
                    inputComponent.jumpJustPressed = pressed
                    inputComponent.jumpIsPressed = pressed
                }

                Action.CROUCH -> inputComponent.crouch = pressed
                Action.ATTACK -> inputComponent.attackJustPressed = pressed
                Action.BASH -> inputComponent.bashJustPressed = pressed
                Action.MOVE_LEFT ->
                    inputComponent.direction =
                        if (!pressed && inputComponent.direction == WalkDirection.LEFT) {
                            WalkDirection.NONE
                        } else if (pressed) {
                            WalkDirection.LEFT
                        } else {
                            inputComponent.direction
                        }

                Action.MOVE_RIGHT ->
                    inputComponent.direction =
                        if (!pressed && inputComponent.direction == WalkDirection.RIGHT) {
                            WalkDirection.NONE
                        } else if (pressed) {
                            WalkDirection.RIGHT
                        } else {
                            inputComponent.direction
                        }

                Action.MESSAGE ->
                    messageDispatcher.dispatchMessage(
                        0f,
                        FsmMessageTypes.HEAL.ordinal,
                        pressed,
                    )

                Action.MESSAGE2 ->
                    messageDispatcher.dispatchMessage(
                        1f,
                        FsmMessageTypes.ATTACK.ordinal,
                        pressed,
                    )

                Action.KILL ->
                    messageDispatcher.dispatchMessage(
                        0f,
                        FsmMessageTypes.KILL.ordinal,
                        pressed,
                    )

                else -> Unit
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
        MESSAGE2,
        DEBUG,
        KILL,
    }
}
