package io.bennyoe.state.minotaur

import com.badlogic.gdx.ai.msg.Telegram
import com.badlogic.gdx.graphics.g2d.Animation
import io.bennyoe.components.AttackType
import io.bennyoe.components.HitEffectComponent
import io.bennyoe.components.animation.MinotaurAnimation
import io.bennyoe.state.AbstractFSM
import io.bennyoe.state.FsmMessageTypes
import ktx.log.logger

const val SCREAM_DURATION = 2f
const val SHAKE_DURATION = 2f
const val STUNNED_DURATION = 1f

@Suppress("ktlint:standard:class-naming")
sealed class MinotaurFSM : AbstractFSM<MinotaurStateContext>() {
    var screamTimeCounter = 0f
    var stunnedTimeCounter = 0f
    var shakeTimeCounter = 0f

    class IDLE : MinotaurFSM() {
        override fun enter(ctx: MinotaurStateContext) {
            ctx.setAnimation(MinotaurAnimation.IDLE)
        }

        override fun update(ctx: MinotaurStateContext) {
            // TODO check for any intention. If one is true -> changeState to SCREAM
            when {
                ctx.wantsToGrabAttack -> ctx.changeState(SPIN_ATTACK_START())
                ctx.wantsToThrowAttack -> ctx.changeState(THROWING_ROCK())
                ctx.wantsToStompAttack -> ctx.changeState(STOMP())
                ctx.wantsToScream -> ctx.changeState(SCREAM())
                ctx.wantsToWalk -> ctx.changeState(WALK())
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
            ctx.setAnimation(MinotaurAnimation.WALK)
        }

        override fun update(ctx: MinotaurStateContext) {
            when {
                hasWaterContact(ctx) -> ctx.changeState(DEATH())
                ctx.wantsToScream -> ctx.changeState(SCREAM())
                ctx.wantsToIdle -> ctx.changeState(IDLE())
            }
        }

        override fun onMessage(
            ctx: MinotaurStateContext,
            telegram: Telegram,
        ): Boolean = super.onMessage(ctx, telegram)
    }

    class SCREAM : MinotaurFSM() {
        override fun enter(ctx: MinotaurStateContext) {
            ctx.rotateToPlayer()
            ctx.setAnimation(MinotaurAnimation.SCREAM)
            screamTimeCounter = 0f
        }

        override fun update(ctx: MinotaurStateContext) {
            if (screamTimeCounter < SCREAM_DURATION) {
                screamTimeCounter += ctx.deltaTime
                return
            }
            ctx.changeState(IDLE())
        }

        override fun exit(ctx: MinotaurStateContext) {
            screamTimeCounter = 0f
        }

        override fun onMessage(
            ctx: MinotaurStateContext,
            telegram: Telegram,
        ): Boolean = super.onMessage(ctx, telegram)
    }

    class SPIN_ATTACK_START : MinotaurFSM() {
        override fun enter(ctx: MinotaurStateContext) {
            ctx.setAnimation(MinotaurAnimation.SPIN_ATTACK_START, Animation.PlayMode.NORMAL)
            ctx.intentionCmp.wantsToGrabAttack = false
        }

        override fun update(ctx: MinotaurStateContext) {
            if (ctx.animationComponent.isAnimationFinished()) {
                ctx.changeState(SPIN_ATTACK_LOOP())
            }
        }

        override fun onMessage(
            ctx: MinotaurStateContext,
            telegram: Telegram,
        ): Boolean = super.onMessage(ctx, telegram)
    }

    class SPIN_ATTACK_LOOP : MinotaurFSM() {
        override fun enter(ctx: MinotaurStateContext) {
            ctx.setAnimation(MinotaurAnimation.SPIN_ATTACK_LOOP)
        }

        override fun update(ctx: MinotaurStateContext) {
            ctx.runTowardsPlayer()
            when {
                ctx.runIntoWall() -> ctx.changeState(STUNNED())
                ctx.runIntoPlayer() -> ctx.changeState(GRABBING())
            }
        }

        override fun exit(ctx: MinotaurStateContext) {
            ctx.intentionCmp.wantsToChase = false
            ctx.stopMovement()
        }

        override fun onMessage(
            ctx: MinotaurStateContext,
            telegram: Telegram,
        ): Boolean = super.onMessage(ctx, telegram)
    }

    class GRABBING : MinotaurFSM() {
        override fun enter(ctx: MinotaurStateContext) {
            ctx.setAnimation(MinotaurAnimation.SHAKING_PLAYER)
            logger.debug { "GRABBING PLAYER" }
        }

        override fun update(ctx: MinotaurStateContext) {
            if (ctx.animationComponent.isAnimationFinished()) {
                ctx.changeState(SHAKING())
            }
            /* TODO
            set player to not moveable
            play animation, then -> SHAKING
             */
        }

        override fun onMessage(
            ctx: MinotaurStateContext,
            telegram: Telegram,
        ): Boolean = super.onMessage(ctx, telegram)
    }

    class SHAKING : MinotaurFSM() {
        override fun enter(ctx: MinotaurStateContext) {
            ctx.setAnimation(MinotaurAnimation.SHAKING_PLAYER)
            logger.debug { "SHAKING PLAYER" }
        }

        override fun update(ctx: MinotaurStateContext) {
            if (shakeTimeCounter < SHAKE_DURATION) {
                shakeTimeCounter += ctx.deltaTime
                return
            }
            ctx.changeState(IDLE())
            /* TODO
            reduce player health
            play animation, then -> THROWING
             */
        }

        override fun onMessage(
            ctx: MinotaurStateContext,
            telegram: Telegram,
        ): Boolean = super.onMessage(ctx, telegram)
    }

    class THROWING_PLAYER : MinotaurFSM() {
        override fun enter(ctx: MinotaurStateContext) {
            ctx.setAnimation(MinotaurAnimation.THROW_PLAYER)
            logger.debug { "THROWING PLAYER" }
        }

        override fun update(ctx: MinotaurStateContext) {
            if (ctx.animationComponent.isAnimationFinished()) {
                ctx.changeState(IDLE())
            }
            /* TODO
            reduce player health
            play animation, then -> THROWING
             */
        }

        override fun onMessage(
            ctx: MinotaurStateContext,
            telegram: Telegram,
        ): Boolean = super.onMessage(ctx, telegram)
    }

    class THROWING_ROCK : MinotaurFSM() {
        private val PICKUP_FRAME_INDEX = 2
        private val RELEASE_FRAME_INDEX = 3

        private var hasSpawnedRock = false
        private var hasThrownRock = false

        override fun enter(ctx: MinotaurStateContext) {
            ctx.rotateToPlayer()
            ctx.intentionCmp.wantsToThrowAttack = false
            ctx.setAnimation(MinotaurAnimation.ROCK_ATTACK, Animation.PlayMode.NORMAL)
            hasSpawnedRock = false
            hasThrownRock = false
        }

        override fun update(ctx: MinotaurStateContext) {
            val aniCmp = ctx.animationComponent
            val currentFrame = aniCmp.animation.getKeyFrameIndex(aniCmp.stateTime)

            if (!hasSpawnedRock && currentFrame >= PICKUP_FRAME_INDEX) {
                ctx.spawnRock()
                hasSpawnedRock = true
            }

            if (!hasThrownRock && currentFrame >= RELEASE_FRAME_INDEX) {
                val playerPos = ctx.getPlayerPosition()
                ctx.throwRock(playerPos)
                hasThrownRock = true
            }

            if (ctx.animationComponent.isAnimationFinished()) ctx.changeState(IDLE())
        }

        override fun exit(ctx: MinotaurStateContext) {
            if (ctx.heldProjectile != null) {
                // remove entity
                with(ctx.world) { ctx.heldProjectile!!.remove() }
                ctx.heldProjectile = null
            }
        }

        override fun onMessage(
            ctx: MinotaurStateContext,
            telegram: Telegram,
        ): Boolean = super.onMessage(ctx, telegram)
    }

    class RECOVER : MinotaurFSM() {
        override fun enter(ctx: MinotaurStateContext) {
        }

        override fun update(ctx: MinotaurStateContext) {
            // TODO play recover animation for 2 sec then IDLE
        }

        override fun onMessage(
            ctx: MinotaurStateContext,
            telegram: Telegram,
        ): Boolean = super.onMessage(ctx, telegram)
    }

    class STUNNED : MinotaurFSM() {
        override fun enter(ctx: MinotaurStateContext) {
            ctx.setAnimation(MinotaurAnimation.STUNNED)
            logger.debug { "STUNNED" }
            stunnedTimeCounter = 0f
        }

        override fun update(ctx: MinotaurStateContext) {
            if (stunnedTimeCounter < STUNNED_DURATION) {
                stunnedTimeCounter += ctx.deltaTime
                return
            }

            ctx.changeState(IDLE())
            /* TODO
            make minotaur vulnerable
            after 2 sec make minotaur invincible and IDLE
             */
        }

        override fun onMessage(
            ctx: MinotaurStateContext,
            telegram: Telegram,
        ): Boolean = super.onMessage(ctx, telegram)
    }

    class STOMP : MinotaurFSM() {
        override fun enter(ctx: MinotaurStateContext) {
            ctx.setAnimation(MinotaurAnimation.STOMP_ATTACK, Animation.PlayMode.NORMAL, true)
            ctx.intentionCmp.wantsToJump = true
            ctx.intentionCmp.wantsToStomp = false
        }

        override fun update(ctx: MinotaurStateContext) {
            if (ctx.animationComponent.isAnimationFinished()) {
                ctx.intentionCmp.wantsToJump = false
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
            ctx.setAnimation(MinotaurAnimation.HIT, resetStateTime = true)
            ctx.resetAllIntentions()
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
                MinotaurAnimation.DYING,
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
