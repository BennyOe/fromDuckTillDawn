package io.bennyoe

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.ai.msg.MessageManager
import com.github.quillraven.fleks.World
import io.bennyoe.components.GameStateComponent
import io.bennyoe.components.InputComponent
import io.bennyoe.components.debug.DebugComponent
import io.bennyoe.state.FsmMessageTypes
import ktx.app.KtxInputAdapter
import ktx.log.logger

class PlayerInputProcessor(
    world: World,
) : KtxInputAdapter {
    private val inputEntities = world.family { all(InputComponent) }
    private val debugEntities = world.family { all(DebugComponent) }
    private val gameStateEntities = world.family { all(GameStateComponent) }
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
            Keys.P to Action.PAUSE,
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
            val debugCmp = debugEntity[DebugComponent]
            when (action) {
                Action.DEBUG -> debugCmp.toggleDebug(pressed)
                else -> Unit
            }
        }
        gameStateEntities.forEach { gameStateEntity ->
            val gameStateCmp = gameStateEntity[GameStateComponent]
            when (action) {
                Action.PAUSE -> gameStateCmp.toggleDebug(pressed)
                else -> Unit
            }
        }
        inputEntities.forEach { input ->
            val inputCmp = input[InputComponent]
            when (action) {
                Action.JUMP -> {
                    inputCmp.jumpJustPressed = pressed
                    inputCmp.jumpIsPressed = pressed
                }

                Action.CROUCH -> inputCmp.crouchJustPressed = pressed
                Action.ATTACK -> inputCmp.attackJustPressed = pressed
                Action.BASH -> inputCmp.bashJustPressed = pressed

                Action.MOVE_LEFT -> inputCmp.walkLeftJustPressed = pressed
                Action.MOVE_RIGHT -> inputCmp.walkRightJustPressed = pressed

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
        PAUSE,
    }
}
