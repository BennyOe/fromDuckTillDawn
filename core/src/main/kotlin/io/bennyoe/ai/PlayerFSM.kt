package io.bennyoe.ai

import com.badlogic.gdx.ai.fsm.State
import com.badlogic.gdx.ai.msg.Telegram
import com.badlogic.gdx.graphics.g2d.Animation
import io.bennyoe.components.AnimationModel
import io.bennyoe.components.AnimationType
import io.bennyoe.components.AnimationVariant
import io.bennyoe.components.JumpComponent
import io.bennyoe.components.WalkDirection

sealed class PlayerFSM : State<StateContext> {
    data object IDLE : PlayerFSM() {
        override fun enter(ctx: StateContext) {
            logger.debug { "Entering IDLE" }
            animation(ctx, AnimationType.IDLE)
        }

        override fun update(ctx: StateContext) {
            if (ctx.inputComponent.jumpJustPressed) {
                ctx.aiComponent.stateMachine.changeState(JUMP)
            }
            if (ctx.inputComponent.crouch) {
                ctx.aiComponent.stateMachine.changeState(CROUCH_IDLE)
            }
            if (ctx.inputComponent.direction != WalkDirection.NONE) {
                ctx.aiComponent.stateMachine.changeState(WALK)
            }
        }
    }

    data object WALK : PlayerFSM() {
        override fun enter(ctx: StateContext) {
            logger.debug { "Entering WALK" }
            animation(ctx, AnimationType.WALK)
        }

        override fun update(ctx: StateContext) {
            if (ctx.inputComponent.direction == WalkDirection.NONE) {
                ctx.aiComponent.stateMachine.changeState(IDLE)
            }
            if (ctx.inputComponent.jumpJustPressed){
                ctx.aiComponent.stateMachine.changeState(JUMP)
            }
        }
    }

    data object JUMP : PlayerFSM() {
        override fun enter(ctx: StateContext) {
            logger.debug { "Entering JUMP" }
            ctx.inputComponent.jumpJustPressed = false
            animation(ctx, AnimationType.JUMP)
            ctx.add(JumpComponent())
        }

        override fun update(ctx: StateContext) {
            if (ctx.inputComponent.jumpJustPressed) {
                ctx.aiComponent.stateMachine.changeState(DOUBLE_JUMP)
            }
            if (ctx.physicComponent.body.linearVelocity.y < 0) {
                ctx.aiComponent.stateMachine.changeState(FALL)
            }
        }
    }

    data object DOUBLE_JUMP : PlayerFSM() {
        override fun enter(ctx: StateContext) {
            logger.debug { "Entering DOUBLE_JUMP" }
            ctx.add(JumpComponent())
            animation(ctx, AnimationType.JUMP)
        }

        override fun update(ctx: StateContext) {
            if (ctx.physicComponent.body.linearVelocity.y < 0) {
                ctx.aiComponent.stateMachine.changeState(FALL)
            }
        }
    }

    data object FALL : PlayerFSM() {
        override fun enter(ctx: StateContext) {
            logger.debug { "Entering FALL" }
            animation(ctx, AnimationType.CROUCH_IDLE)
        }

        override fun update(ctx: StateContext) {
            if (ctx.inputComponent.jumpJustPressed && ctx.aiComponent.stateMachine.previousState != DOUBLE_JUMP) {
                ctx.aiComponent.stateMachine.changeState(DOUBLE_JUMP)
            }
            if (ctx.physicComponent.body.linearVelocity.y >= 0) {
                ctx.aiComponent.stateMachine.changeState(IDLE)
            }
        }
    }

    data object CROUCH_IDLE : PlayerFSM() {
        override fun enter(ctx: StateContext) {
            logger.debug { "Entering CROUCH_IDLE" }
            animation(ctx, AnimationType.CROUCH_IDLE)
        }

        override fun update(ctx: StateContext) {
            if (!ctx.inputComponent.crouch) {
                ctx.aiComponent.stateMachine.changeState(IDLE)
            }
            if (ctx.inputComponent.direction != WalkDirection.NONE) {
                ctx.aiComponent.stateMachine.changeState(CROUCH_WALK)
            }
        }
    }

    data object CROUCH_WALK : PlayerFSM() {
        override fun enter(ctx: StateContext) {
            logger.debug { "Entering CROUCH_WALK" }
            animation(ctx, AnimationType.CROUCH_WALK)
        }

        override fun update(ctx: StateContext) {
            if (ctx.inputComponent.direction == WalkDirection.NONE) {
                ctx.aiComponent.stateMachine.changeState(CROUCH_IDLE)
            }
        }
    }

    data object ATTACK_1 : PlayerFSM() {
        override fun enter(ctx: StateContext) {
            logger.debug { "Entering ATTACK_1" }
            animation(ctx, AnimationType.ATTACK)
        }

        override fun update(ctx: StateContext) {
            // TODO NOT IMPLEMENTED
        }
    }

    data object ATTACK_2 : PlayerFSM() {
        override fun enter(ctx: StateContext) {
            logger.debug { "Entering ATTACK_2" }
            animation(ctx, AnimationType.ATTACK, variant = AnimationVariant.SECOND)
        }

        override fun update(ctx: StateContext) {
            // TODO NOT IMPLEMENTED
        }
    }

    data object ATTACK3 : PlayerFSM() {
        override fun enter(ctx: StateContext) {
            logger.debug { "Entering ATTACK_3" }
            animation(ctx, AnimationType.ATTACK, variant = AnimationVariant.THIRD)
        }

        override fun update(ctx: StateContext) {
            // TODO NOT IMPLEMENTED
        }
    }

    data object BASH : PlayerFSM() {
        override fun enter(ctx: StateContext) {
            logger.debug { "Entering BASH" }
            animation(ctx, AnimationType.BASH)
        }

        override fun update(ctx: StateContext) {
            // TODO NOT IMPLEMENTED
        }
    }

    fun animation(
        ctx: StateContext,
        type: AnimationType,
        playMode: Animation.PlayMode = Animation.PlayMode.LOOP,
        variant: AnimationVariant = AnimationVariant.FIRST,
    ) {
        ctx.animationComponent.nextAnimation(AnimationModel.PLAYER_DAWN, type, variant)
        ctx.animationComponent.animation.playMode = playMode
    }

    override fun enter(ctx: StateContext) = Unit
    override fun update(ctx: StateContext) = Unit
    override fun exit(ctx: StateContext) = Unit
    override fun onMessage(ctx: StateContext, telegram: Telegram) = false

    companion object {
        val logger = ktx.log.logger<PlayerFSM>()
    }
}
