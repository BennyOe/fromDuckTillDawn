package io.bennyoe.state.mushroom

import com.badlogic.gdx.ai.msg.Telegram
import com.badlogic.gdx.graphics.g2d.Animation
import io.bennyoe.components.AnimationType
import io.bennyoe.components.AnimationVariant
import io.bennyoe.state.AbstractFSM
import io.bennyoe.state.FsmMessageTypes
import io.bennyoe.state.LANDING_VELOCITY_EPS
import ktx.log.logger
import kotlin.math.abs

sealed class MushroomFSM : AbstractFSM<MushroomStateContext>() {
    data object IDLE : MushroomFSM() {
        override fun enter(ctx: MushroomStateContext) {
            ctx.setAnimation(AnimationType.IDLE)
        }

        override fun update(ctx: MushroomStateContext) {
            when {
                ctx.wantsToWalk -> ctx.changeState(WALK)
                ctx.wantsToAttack -> ctx.changeState(ATTACK)
                ctx.wantsToJump -> ctx.changeState(JUMP)
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
                ctx.wantsToJump -> ctx.changeState(JUMP)
            }
        }

        override fun onMessage(
            ctx: MushroomStateContext,
            telegram: Telegram,
        ): Boolean = super.onMessage(ctx, telegram)
    }

    data object JUMP : MushroomFSM() {
        override fun enter(ctx: MushroomStateContext) {
            ctx.jumpComponent.wantsToJump = true
            ctx.intentionCmp.wantsToJump = false
            ctx.setAnimation(AnimationType.JUMP)
        }

        override fun update(ctx: MushroomStateContext) {
            when {
                isFalling(ctx) -> ctx.changeState(FALL)
            }
        }
    }

    data object FALL : MushroomFSM() {
        override fun enter(ctx: MushroomStateContext) {
            ctx.setAnimation(AnimationType.JUMP)
        }

        override fun update(ctx: MushroomStateContext) {
            val velY = ctx.physicComponent.body.linearVelocity.y
            when {
                // Land only when we actually touch the ground *and* vertical speed is ~0
                abs(velY) <= LANDING_VELOCITY_EPS -> ctx.changeState(IDLE)
                // otherwise remain in FALL
                else -> ctx.intentionCmp.wantsToJump = false
            }
        }
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
            ctx.attackCmp.applyAttack = false
            ctx.moveComponent.lockMovement = true
            ctx.healthComponent.takenDamage = 0f
        }

        override fun update(ctx: MushroomStateContext) {
            if (ctx.animationComponent.isAnimationFinished()) {
                ctx.moveComponent.lockMovement = false
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
                isReversed = ctx.deathAlreadyEnteredBefore,
            )
            ctx.entityIsDead(true, 2f)
        }
    }

    override fun enter(ctx: MushroomStateContext) = Unit

    override fun update(ctx: MushroomStateContext) = Unit

    override fun exit(ctx: MushroomStateContext) = Unit

    override fun onMessage(
        ctx: MushroomStateContext,
        telegram: Telegram,
    ): Boolean {
        if (telegram.message == FsmMessageTypes.ENEMY_IS_HIT.ordinal) {
            ctx.changeState(HIT)
            return true
        }
        return false
    }

    companion object {
        val logger = logger<MushroomFSM>()
    }
}
