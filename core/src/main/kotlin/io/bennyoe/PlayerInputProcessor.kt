package io.bennyoe

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.ai.msg.MessageManager
import com.github.quillraven.fleks.World
import io.bennyoe.components.CameraComponent
import io.bennyoe.components.GameStateComponent
import io.bennyoe.components.InputComponent
import io.bennyoe.components.PlayerComponent
import io.bennyoe.components.StateComponent
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
    private val cameraEntities = world.family { all(CameraComponent) }
    private val playerEntity = world.family { all(PlayerComponent) }.first()
    private val messageDispatcher = MessageManager.getInstance()

    // map that explicitly allows certain actions in specific states
    private val allowedActionsPerState: Map<String, Set<Action>> =
        mapOf(
            "IDLE" to Action.entries.toSet(),
            "WALK" to Action.entries.toSet(),
            "JUMP" to Action.entries.toSet(),
            "DOUBLE_JUMP" to Action.entries.toSet(),
            "FALL" to setOf(Action.JUMP, Action.MOVE_LEFT, Action.MOVE_RIGHT),
            "CROUCH_IDLE" to setOf(Action.MOVE_LEFT, Action.MOVE_RIGHT, Action.CROUCH),
            "CROUCH_WALK" to setOf(Action.MOVE_LEFT, Action.MOVE_RIGHT, Action.CROUCH),
            "ATTACK_1" to Action.entries.toSet(),
            "ATTACK_2" to Action.entries.toSet(),
            "ATTACK_3" to Action.entries.toSet(),
            "BASH" to emptySet(),
            "HIT" to Action.entries.toSet(),
            "DEAD" to emptySet(),
        )

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
            Keys.UP to Action.ZOOM_IN,
            Keys.DOWN to Action.ZOOM_OUT,
            Keys.F to Action.TOGGLE_FLASHLIGHT,
            Keys.X to Action.TOGGLE_DAY_NIGHT,
            Keys.Z to Action.TOGGLE_WEATHER,
            Keys.SHIFT_LEFT to Action.FIRE_LIGHTNING,
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
                Action.PAUSE -> gameStateCmp.togglePause(pressed)
                Action.TOGGLE_DAY_NIGHT -> gameStateCmp.toggleTimeOfDayChange(pressed)
                Action.TOGGLE_WEATHER -> gameStateCmp.toggleWeatherChange(pressed)
                Action.FIRE_LIGHTNING -> gameStateCmp.fireLightning(pressed)
                else -> Unit
            }
        }
        cameraEntities.forEach { cameraEntity ->
            val cameraCmp = cameraEntity[CameraComponent]
            when (action) {
                Action.ZOOM_IN -> {
                    cameraCmp.zoomFactor -= 0.05f
                    logger.debug { "Camera zoom: ${cameraCmp.zoomFactor}" }
                }

                Action.ZOOM_OUT -> {
                    cameraCmp.zoomFactor += 0.05f
                    logger.debug { "Camera zoom: ${cameraCmp.zoomFactor}" }
                }

                else -> Unit
            }
        }
        inputEntities.forEach { input ->
            val playerState = playerEntity[StateComponent].stateMachine.currentState.toString()
            val inputCmp = input[InputComponent]
            val allowed = allowedActionsPerState[playerState] ?: emptySet()
            logger.debug { "playerState $playerState" }
            if (pressed && action != Action.KILL && action !in allowed) return@forEach

            when (action) {
                Action.JUMP -> {
                    inputCmp.jumpJustPressed = pressed
                    inputCmp.jumpIsPressed = pressed
                }

                Action.CROUCH -> {
                    inputCmp.crouchJustPressed = pressed
                }

                Action.ATTACK -> inputCmp.attackJustPressed = pressed
                Action.BASH -> inputCmp.bashJustPressed = pressed

                Action.MOVE_LEFT -> inputCmp.walkLeftJustPressed = pressed
                Action.MOVE_RIGHT -> inputCmp.walkRightJustPressed = pressed

                Action.TOGGLE_FLASHLIGHT -> inputCmp.flashlightToggleJustPressed = pressed

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
        ZOOM_IN,
        ZOOM_OUT,
        TOGGLE_FLASHLIGHT,
        TOGGLE_DAY_NIGHT,
        TOGGLE_WEATHER,
        FIRE_LIGHTNING,
    }
}
