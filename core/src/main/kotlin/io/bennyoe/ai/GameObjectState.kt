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
            if (context.inputComponent.jump) {
                context.aiComponent.stateMachine.changeState(JUMP)
            }
            if (context.inputComponent.crouch) {
                context.aiComponent.stateMachine.changeState(CROUCH_IDLE)
            }
        }
    }

    data object WALK : GameObjectState() {
        override fun enter(context: StateContext) {
            logger.debug { "Entering WALK" }
            animation(context, AnimationType.WALK)
        }

        override fun update(context: StateContext) {
            // TODO NOT IMPLEMENTED
        }
    }

    data object JUMP : GameObjectState() {
        override fun enter(context: StateContext) {
            logger.debug { "Entering JUMP" }
            animation(context, AnimationType.JUMP)
        }

        override fun update(context: StateContext) {
            if (context.animationComponent.animation.isAnimationFinished(context.animationComponent.stateTime)) {
                if (!context.inputComponent.jump) {
                    context.aiComponent.stateMachine.changeState(IDLE)
                }
            }
        }
    }

    data object DOUBLE_JUMP : GameObjectState() {
        override fun enter(context: StateContext) {
            logger.debug { "Entering DOUBLE_JUMP" }
            animation(context, AnimationType.JUMP)
        }

        override fun update(context: StateContext) {
            // TODO NOT IMPLEMENTED
        }
    }

    data object FALL : GameObjectState() {
        override fun enter(context: StateContext) {
            logger.debug { "Entering FALL" }
            animation(context, AnimationType.CROUCH_IDLE)
        }

        override fun update(context: StateContext) {
            // TODO NOT IMPLEMENTED
        }
    }

    data object CROUCH_IDLE : GameObjectState() {
        override fun enter(context: StateContext) {
            logger.debug { "Entering CROUCH_IDLE" }
            animation(context, AnimationType.CROUCH_IDLE)
        }

        override fun update(context: StateContext) {
            if (context.animationComponent.animation.isAnimationFinished(context.animationComponent.stateTime)) {
                if (!context.inputComponent.crouch) {
                    context.aiComponent.stateMachine.changeState(IDLE)
                }
            }
        }
    }

    data object CROUCH_WALK : GameObjectState() {
        override fun enter(context: StateContext) {
            logger.debug { "Entering CROUCH_WALK" }
            animation(context, AnimationType.CROUCH_WALK)
        }

        override fun update(context: StateContext) {
            // TODO NOT IMPLEMENTED
        }
    }

    data object ATTACK_1 : GameObjectState() {
        override fun enter(context: StateContext) {
            logger.debug { "Entering ATTACK_1" }
            animation(context, AnimationType.ATTACK)
        }

        override fun update(context: StateContext) {
            // TODO NOT IMPLEMENTED
        }
    }

    data object ATTACK_2 : GameObjectState() {
        override fun enter(context: StateContext) {
            logger.debug { "Entering ATTACK_2" }
            animation(context, AnimationType.ATTACK, variant = AnimationVariant.SECOND)
        }

        override fun update(context: StateContext) {
            // TODO NOT IMPLEMENTED
        }
    }

    data object ATTACK3 : GameObjectState() {
        override fun enter(context: StateContext) {
            logger.debug { "Entering ATTACK_3" }
            animation(context, AnimationType.ATTACK, variant = AnimationVariant.THIRD)
        }

        override fun update(context: StateContext) {
            // TODO NOT IMPLEMENTED
        }
    }

    data object BASH : GameObjectState() {
        override fun enter(context: StateContext) {
            logger.debug { "Entering BASH" }
            animation(context, AnimationType.BASH)
        }

        override fun update(context: StateContext) {
            // TODO NOT IMPLEMENTED
        }
    }
    fun animation(
        context: StateContext,
        type: AnimationType,
        playMode: Animation.PlayMode = Animation.PlayMode.LOOP,
        variant: AnimationVariant = AnimationVariant.FIRST,
    ) {
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
