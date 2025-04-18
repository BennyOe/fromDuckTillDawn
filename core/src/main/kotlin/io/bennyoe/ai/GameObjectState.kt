package io.bennyoe.ai

import com.badlogic.gdx.ai.fsm.State
import com.badlogic.gdx.ai.msg.Telegram
import com.badlogic.gdx.graphics.g2d.Animation
import io.bennyoe.components.AnimationModel
import io.bennyoe.components.AnimationType
import io.bennyoe.components.AnimationVariant
import io.bennyoe.components.StateContext

sealed class GameObjectState : State<StateContext> {

    data object IDLE : GameObjectState() {
        override fun enter(context: StateContext) {
            logger.debug { "Entering IDLE" }
            animation(context, AnimationType.IDLE)
        }

        override fun update(context: StateContext) {
            if (context.aiComponent.nextStateIntent == JUMP) {
                context.aiComponent.stateMachine.changeState(JUMP)
            }
            if (context.aiComponent.nextStateIntent == CROUCH) {
                context.aiComponent.stateMachine.changeState(CROUCH)
            }
        }
    }

    data object JUMP : GameObjectState() {
        override fun enter(context: StateContext) {
            logger.debug { "Entering JUMP" }
            animation(context, AnimationType.JUMP)
        }

        override fun update(context: StateContext) {
            if (context.animationComponent.animation.isAnimationFinished(context.animationComponent.stateTime)) {
                if (context.aiComponent.nextStateIntent == IDLE) {
                    context.aiComponent.stateMachine.changeState(IDLE)
                }
            }
        }
    }

    data object CROUCH : GameObjectState() {
        override fun enter(context: StateContext) {
            logger.debug { "Entering Crouch" }
            animation(context, AnimationType.CROUCH_IDLE)
        }

        override fun update(context: StateContext) {
            if (context.animationComponent.animation.isAnimationFinished(context.animationComponent.stateTime)) {
                if (context.aiComponent.nextStateIntent == IDLE) {
                    context.aiComponent.stateMachine.changeState(IDLE)
                }
            }
        }
    }

    fun animation(context: StateContext, type: AnimationType, playMode: Animation.PlayMode = Animation.PlayMode.LOOP) {
        context.animationComponent.nextAnimation(AnimationModel.PLAYER_DAWN, type, AnimationVariant.FIRST)
        context.animationComponent.animation.playMode = playMode
    }

    override fun enter(context: StateContext) = Unit
    override fun update(context: StateContext) = Unit
    override fun exit(context: StateContext) = Unit
    override fun onMessage(context: StateContext, telegram: Telegram) = false

    companion object {
        val logger = ktx.log.logger<GameObjectState>()
    }
}
