package io.bennyoe.state.minotaur

import com.badlogic.gdx.ai.msg.Telegram
import com.badlogic.gdx.graphics.g2d.Animation
import io.bennyoe.components.AnimationType
import io.bennyoe.components.AttackType
import io.bennyoe.components.HitEffectComponent
import io.bennyoe.state.AbstractFSM
import io.bennyoe.state.FsmMessageTypes
import ktx.log.logger

sealed class MinotaurFSM : AbstractFSM<MinotaurStateContext>() {
    class IDLE : MinotaurFSM() {
        override fun enter(ctx: MinotaurStateContext) {
            ctx.setAnimation(AnimationType.IDLE)
        }

        override fun update(ctx: MinotaurStateContext) {
            when {
                ctx.wantsToWalk -> ctx.changeState(WALK())
                ctx.wantsToAttack -> ctx.changeState(ATTACK())
                hasWaterContact(ctx) -> ctx.changeState(DEATH())
            }
        }

        override fun onMessage(
            ctx: MinotaurStateContext,
            telegram: Telegram,
        ): Boolean = super.onMessage(ctx, telegram)
    }

    class WALK : MinotaurFSM() {
        override fun enter(ctx: MinotaurStateContext) {
            ctx.setAnimation(AnimationType.WALK)
        }

        override fun update(ctx: MinotaurStateContext) {
            when {
                hasWaterContact(ctx) -> ctx.changeState(DEATH())
                ctx.wantsToAttack -> ctx.changeState(ATTACK())
                ctx.wantsToIdle -> ctx.changeState(IDLE())
            }
        }

        override fun onMessage(
            ctx: MinotaurStateContext,
            telegram: Telegram,
        ): Boolean = super.onMessage(ctx, telegram)
    }

    class ATTACK : MinotaurFSM() {
        override fun enter(ctx: MinotaurStateContext) {
            ctx.setAnimation(AnimationType.ATTACK_1)
            ctx.attackCmp.appliedAttack = AttackType.HEADNUT
        }

        override fun update(ctx: MinotaurStateContext) {
            if (hasWaterContact(ctx)) ctx.changeState(DEATH())
            if (ctx.animationComponent.isAnimationFinished()) {
                ctx.changeState(IDLE())
            }
        }

        override fun onMessage(
            ctx: MinotaurStateContext,
            telegram: Telegram,
        ): Boolean = super.onMessage(ctx, telegram)
    }

    class HIT : MinotaurFSM() {
        override fun enter(ctx: MinotaurStateContext) {
            ctx.add(HitEffectComponent())
            ctx.setAnimation(AnimationType.HIT, resetStateTime = true)
            ctx.attackCmp.appliedAttack = AttackType.NONE
            ctx.moveComponent.lockMovement = true
            ctx.moveComponent.moveVelocity.x = 0f
            ctx.healthComponent.takenDamage = 0f
        }

        override fun update(ctx: MinotaurStateContext) {
            if (ctx.animationComponent.isAnimationFinished()) {
                ctx.moveComponent.lockMovement = false
                ctx.changeState(IDLE())
            }
        }
    }

    class DEATH : MinotaurFSM() {
        override fun enter(ctx: MinotaurStateContext) {
            ctx.healthComponent.current = 0f
            ctx.setAnimation(
                AnimationType.DYING,
                Animation.PlayMode.NORMAL,
                // isReversed has to be set after the first time to prevent flickering because animation is played back reversed in RESURRECT state
                isReversed = ctx.deathAlreadyEnteredBefore,
            )
            ctx.entityIsDead(true, 2f)
        }

        override fun onMessage(
            ctx: MinotaurStateContext,
            telegram: Telegram,
        ): Boolean = false
    }

    override fun enter(ctx: MinotaurStateContext) = Unit

    override fun update(ctx: MinotaurStateContext) = Unit

    override fun exit(ctx: MinotaurStateContext) = Unit

    override fun onMessage(
        ctx: MinotaurStateContext,
        telegram: Telegram,
    ): Boolean {
        if (telegram.message == FsmMessageTypes.ENEMY_IS_HIT.ordinal) {
            ctx.changeState(HIT())
            return true
        }
        return false
    }

    companion object {
        val logger = logger<MinotaurFSM>()
    }
}
