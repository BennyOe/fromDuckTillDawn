package io.bennyoe.state.mushroom

import com.badlogic.gdx.ai.msg.Telegram
import com.badlogic.gdx.graphics.g2d.Animation
import io.bennyoe.components.AnimationType
import io.bennyoe.components.AnimationVariant
import io.bennyoe.state.AbstractFSM
import io.bennyoe.state.FsmMessageTypes
import ktx.log.logger

sealed class MushroomFSM : AbstractFSM<MushroomStateContext>() {
    data object IDLE : MushroomFSM() {
        override fun enter(ctx: MushroomStateContext) {
            ctx.setAnimation(AnimationType.IDLE)
        }

        override fun update(ctx: MushroomStateContext) {
            when {
                ctx.wantsToWalk -> ctx.changeState(WALK)
                ctx.wantsToAttack -> ctx.changeState(ATTACK)
            }
        }

        override fun onMessage(
            ctx: MushroomStateContext,
            telegram: Telegram,
        ): Boolean = super.onMessage(ctx, telegram)
    }

    data object WALK : MushroomFSM() {
        override fun enter(ctx: MushroomStateContext) {
            ctx.setAnimation(AnimationType.WALK)
        }

        override fun update(ctx: MushroomStateContext) {
            when {
                ctx.wantsToAttack -> ctx.changeState(ATTACK)
                ctx.wantsToIdle -> ctx.changeState(IDLE)
            }
        }

        override fun onMessage(
            ctx: MushroomStateContext,
            telegram: Telegram,
        ): Boolean = super.onMessage(ctx, telegram)
    }

    data object ATTACK : MushroomFSM() {
        override fun enter(ctx: MushroomStateContext) {
            ctx.setAnimation(AnimationType.ATTACK)
            ctx.attackCmp.applyAttack = true
        }

        override fun update(ctx: MushroomStateContext) {
            if (ctx.animationComponent.isAnimationFinished()) {
                ctx.changeState(IDLE)
            }
        }

        override fun onMessage(
            ctx: MushroomStateContext,
            telegram: Telegram,
        ): Boolean = super.onMessage(ctx, telegram)
    }

    data object HIT : MushroomFSM() {
        override fun enter(ctx: MushroomStateContext) {
            ctx.setAnimation(AnimationType.HIT, resetStateTime = true)
            ctx.healthComponent.takenDamage = 0f
        }

        override fun update(ctx: MushroomStateContext) {
            if (ctx.animationComponent.isAnimationFinished()) {
                ctx.changeState(IDLE)
            }
        }
    }

    data object DEATH : MushroomFSM() {
        override fun enter(ctx: MushroomStateContext) {
            ctx.setAnimation(
                AnimationType.DYING,
                Animation.PlayMode.NORMAL,
                AnimationVariant.FIRST,
                // isReversed has to be set after the first time to prevent flickering because animation is played back reversed in RESURRECT state
                isReversed = deathAlreadyEnteredBefore,
            )
            ctx.moveComponent.lockMovement = true
            ctx.stateComponent.stateMachine.globalState = null
            ctx.moveComponent.moveVelocity = 0f
        }
    }

    override fun enter(ctx: MushroomStateContext) = Unit

    override fun update(ctx: MushroomStateContext) = Unit

    override fun exit(ctx: MushroomStateContext) = Unit

    override fun onMessage(
        ctx: MushroomStateContext,
        telegram: Telegram,
    ): Boolean {
        if (telegram.message == FsmMessageTypes.HIT.ordinal) {
            ctx.changeState(HIT)
            return true
        }
        return false
    }

    companion object {
        val logger = logger<MushroomFSM>()
    }
}
