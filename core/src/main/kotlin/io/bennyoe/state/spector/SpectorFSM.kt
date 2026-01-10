package io.bennyoe.state.spector

import com.badlogic.gdx.ai.msg.Telegram
import com.badlogic.gdx.graphics.g2d.Animation
import io.bennyoe.components.AttackType
import io.bennyoe.components.HitEffectComponent
import io.bennyoe.components.animation.SpectorAnimation
import io.bennyoe.state.AbstractFSM
import io.bennyoe.state.FsmMessageTypes
import io.bennyoe.state.LANDING_VELOCITY_EPS
import ktx.log.logger
import kotlin.math.abs

sealed class SpectorFSM : AbstractFSM<SpectorStateContext>() {
    class IDLE : SpectorFSM() {
        override fun enter(ctx: SpectorStateContext) {
            ctx.setAnimation(SpectorAnimation.IDLE)
        }

        override fun update(ctx: SpectorStateContext) {
            when {
                ctx.wantsToWalk -> ctx.changeState(WALK())
                ctx.wantsToAttack -> ctx.changeState(ATTACK())
                ctx.wantsToJump -> ctx.changeState(JUMP())
                hasWaterContact(ctx) -> ctx.changeState(DEATH())
            }
        }

        override fun onMessage(
            ctx: SpectorStateContext,
            telegram: Telegram,
        ): Boolean = super.onMessage(ctx, telegram)
    }

    class WALK : SpectorFSM() {
        override fun enter(ctx: SpectorStateContext) {
            ctx.setAnimation(SpectorAnimation.WALK)
        }

        override fun update(ctx: SpectorStateContext) {
            when {
                hasWaterContact(ctx) -> ctx.changeState(DEATH())
                ctx.wantsToAttack -> ctx.changeState(ATTACK())
                ctx.wantsToIdle -> ctx.changeState(IDLE())
                ctx.wantsToJump -> ctx.changeState(JUMP())
            }
        }

        override fun onMessage(
            ctx: SpectorStateContext,
            telegram: Telegram,
        ): Boolean = super.onMessage(ctx, telegram)
    }

    class JUMP : SpectorFSM() {
        override fun enter(ctx: SpectorStateContext) {
            ctx.jumpComponent.wantsToJump = true
            ctx.intentionCmp.wantsToJump = false
            ctx.setAnimation(SpectorAnimation.JUMP)
        }

        override fun update(ctx: SpectorStateContext) {
            when {
                isFalling(ctx) -> ctx.changeState(FALL())
            }
        }
    }

    class FALL : SpectorFSM() {
        override fun enter(ctx: SpectorStateContext) {
            ctx.setAnimation(SpectorAnimation.JUMP)
        }

        override fun update(ctx: SpectorStateContext) {
            val velY = ctx.physicComponent.body.linearVelocity.y
            when {
                hasWaterContact(ctx) -> ctx.changeState(DEATH())

                // Land only when we actually touch the ground *and* vertical speed is ~0
                abs(velY) <= LANDING_VELOCITY_EPS -> ctx.changeState(IDLE())

                // otherwise remain in FALL
                else -> ctx.intentionCmp.wantsToJump = false
            }
        }
    }

    class ATTACK : SpectorFSM() {
        override fun enter(ctx: SpectorStateContext) {
            ctx.setAnimation(SpectorAnimation.ATTACK_1)
            ctx.attackCmp.appliedAttack = AttackType.SWORD
        }

        override fun update(ctx: SpectorStateContext) {
            if (hasWaterContact(ctx)) ctx.changeState(DEATH())
            if (ctx.animationComponent.isAnimationFinished()) {
                ctx.changeState(IDLE())
            }
        }

        override fun onMessage(
            ctx: SpectorStateContext,
            telegram: Telegram,
        ): Boolean = super.onMessage(ctx, telegram)
    }

    class HIT : SpectorFSM() {
        override fun enter(ctx: SpectorStateContext) {
            ctx.add(HitEffectComponent())
            ctx.setAnimation(SpectorAnimation.HIT, resetStateTime = true)
            ctx.attackCmp.appliedAttack = AttackType.NONE
            ctx.moveComponent.lockMovement = true
            ctx.moveComponent.moveVelocity.x = 0f
            ctx.healthComponent.takenDamage = 0f
        }

        override fun update(ctx: SpectorStateContext) {
            if (ctx.animationComponent.isAnimationFinished()) {
                ctx.moveComponent.lockMovement = false
                ctx.changeState(IDLE())
            }
        }
    }

    class DEATH : SpectorFSM() {
        override fun enter(ctx: SpectorStateContext) {
            ctx.healthComponent.current = 0f
            ctx.setAnimation(
                SpectorAnimation.DYING,
                Animation.PlayMode.NORMAL,
                // isReversed has to be set after the first time to prevent flickering because animation is played back reversed in RESURRECT state
                isReversed = ctx.deathAlreadyEnteredBefore,
            )
            ctx.entityIsDead(true, 2f)
        }

        override fun onMessage(
            ctx: SpectorStateContext,
            telegram: Telegram,
        ): Boolean = false
    }

    override fun enter(ctx: SpectorStateContext) = Unit

    override fun update(ctx: SpectorStateContext) = Unit

    override fun exit(ctx: SpectorStateContext) = Unit

    override fun onMessage(
        ctx: SpectorStateContext,
        telegram: Telegram,
    ): Boolean {
        if (telegram.message == FsmMessageTypes.ENEMY_IS_HIT.ordinal) {
            ctx.changeState(HIT())
            return true
        }
        return false
    }

    companion object {
        val logger = logger<SpectorFSM>()
    }
}
