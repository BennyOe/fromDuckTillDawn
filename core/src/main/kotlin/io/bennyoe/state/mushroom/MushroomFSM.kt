package io.bennyoe.state.mushroom

import com.badlogic.gdx.ai.msg.Telegram
import com.badlogic.gdx.graphics.g2d.Animation
import io.bennyoe.components.AttackType
import io.bennyoe.components.HitEffectComponent
import io.bennyoe.components.animation.MushroomAnimation
import io.bennyoe.state.AbstractFSM
import io.bennyoe.state.FsmMessageTypes
import io.bennyoe.state.LANDING_VELOCITY_EPS
import ktx.log.logger
import kotlin.math.abs

sealed class MushroomFSM : AbstractFSM<MushroomStateContext>() {
    class IDLE : MushroomFSM() {
        override fun enter(ctx: MushroomStateContext) {
            ctx.setAnimation(MushroomAnimation.IDLE)
        }

        override fun update(ctx: MushroomStateContext) {
            when {
                ctx.wantsToWalk -> ctx.changeState(WALK())
                ctx.wantsToAttack -> ctx.changeState(ATTACK())
                ctx.wantsToJump -> ctx.changeState(JUMP())
                hasWaterContact(ctx) -> ctx.changeState(DEATH())
            }
        }

        override fun onMessage(
            ctx: MushroomStateContext,
            telegram: Telegram,
        ): Boolean = super.onMessage(ctx, telegram)
    }

    class WALK : MushroomFSM() {
        override fun enter(ctx: MushroomStateContext) {
            ctx.setAnimation(MushroomAnimation.WALK)
        }

        override fun update(ctx: MushroomStateContext) {
            when {
                hasWaterContact(ctx) -> ctx.changeState(DEATH())
                ctx.wantsToAttack -> ctx.changeState(ATTACK())
                ctx.wantsToIdle -> ctx.changeState(IDLE())
                ctx.wantsToJump -> ctx.changeState(JUMP())
            }
        }

        override fun onMessage(
            ctx: MushroomStateContext,
            telegram: Telegram,
        ): Boolean = super.onMessage(ctx, telegram)
    }

    class JUMP : MushroomFSM() {
        override fun enter(ctx: MushroomStateContext) {
            ctx.jumpComponent.wantsToJump = true
            ctx.intentionCmp.wantsToJump = false
            ctx.setAnimation(MushroomAnimation.JUMP)
        }

        override fun update(ctx: MushroomStateContext) {
            when {
                isFalling(ctx) -> ctx.changeState(FALL())
            }
        }
    }

    class FALL : MushroomFSM() {
        override fun enter(ctx: MushroomStateContext) {
            ctx.setAnimation(MushroomAnimation.JUMP)
        }

        override fun update(ctx: MushroomStateContext) {
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

    class ATTACK : MushroomFSM() {
        override fun enter(ctx: MushroomStateContext) {
            ctx.setAnimation(MushroomAnimation.ATTACK_1)
            ctx.attackCmp.appliedAttack = AttackType.HEADNUT
        }

        override fun update(ctx: MushroomStateContext) {
            if (hasWaterContact(ctx)) ctx.changeState(DEATH())
            if (ctx.animationComponent.isAnimationFinished()) {
                ctx.changeState(IDLE())
            }
        }

        override fun onMessage(
            ctx: MushroomStateContext,
            telegram: Telegram,
        ): Boolean = super.onMessage(ctx, telegram)
    }

    class HIT : MushroomFSM() {
        override fun enter(ctx: MushroomStateContext) {
            ctx.add(HitEffectComponent())
            ctx.setAnimation(MushroomAnimation.HIT, resetStateTime = true)
            ctx.attackCmp.appliedAttack = AttackType.NONE
            ctx.moveComponent.lockMovement = true
            ctx.moveComponent.moveVelocity.x = 0f
            ctx.healthComponent.takenDamage = 0f
        }

        override fun update(ctx: MushroomStateContext) {
            if (ctx.animationComponent.isAnimationFinished()) {
                ctx.moveComponent.lockMovement = false
                ctx.changeState(IDLE())
            }
        }
    }

    class DEATH : MushroomFSM() {
        override fun enter(ctx: MushroomStateContext) {
            ctx.healthComponent.current = 0f
            ctx.setAnimation(
                MushroomAnimation.DYING,
                Animation.PlayMode.NORMAL,
                // isReversed has to be set after the first time to prevent flickering because animation is played back reversed in RESURRECT state
                isReversed = ctx.deathAlreadyEnteredBefore,
            )
            ctx.entityIsDead(true, 2f)
        }

        override fun onMessage(
            ctx: MushroomStateContext,
            telegram: Telegram,
        ): Boolean = false
    }

    override fun enter(ctx: MushroomStateContext) = Unit

    override fun update(ctx: MushroomStateContext) = Unit

    override fun exit(ctx: MushroomStateContext) = Unit

    override fun onMessage(
        ctx: MushroomStateContext,
        telegram: Telegram,
    ): Boolean {
        if (telegram.message == FsmMessageTypes.ENEMY_IS_HIT.ordinal) {
            ctx.changeState(HIT())
            return true
        }
        return false
    }

    companion object {
        val logger = logger<MushroomFSM>()
    }
}
