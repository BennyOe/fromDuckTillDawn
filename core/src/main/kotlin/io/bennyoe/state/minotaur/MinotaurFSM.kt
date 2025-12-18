package io.bennyoe.state.minotaur

import com.badlogic.gdx.ai.msg.Telegram
import com.badlogic.gdx.graphics.g2d.Animation
import io.bennyoe.components.AttackType
import io.bennyoe.components.HitEffectComponent
import io.bennyoe.components.animation.MinotaurAnimation
import io.bennyoe.state.AbstractFSM
import io.bennyoe.state.FsmMessageTypes
import ktx.log.logger

const val SCREAM_DURATION = 1f
const val SHAKE_DURATION = 2f
const val STUNNED_DURATION = 3f

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
            ctx.prepareCharge()
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
            ctx.chargeForward()
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
            ctx.grabPlayer()
        }

        override fun onMessage(
            ctx: MinotaurStateContext,
            telegram: Telegram,
        ): Boolean = super.onMessage(ctx, telegram)
    }

    class SHAKING : MinotaurFSM() {
        private var damageTickTimer = 0f

        override fun enter(ctx: MinotaurStateContext) {
            ctx.setAnimation(MinotaurAnimation.SHAKING_PLAYER)
            logger.debug { "SHAKING PLAYER" }
            damageTickTimer = 0f
        }

        override fun update(ctx: MinotaurStateContext) {
            damageTickTimer += ctx.deltaTime

            // Retrieve attack config to avoid hardcoded values
            val shakeAttack = ctx.attackCmp.attackMap[AttackType.SHAKE]
            // Default to 0.5s if not configured
            val tickRate = shakeAttack?.attackDelay ?: 0.5f

            if (damageTickTimer >= tickRate) {
                damageTickTimer -= tickRate

                // Apply damage to player
                // We access the player entity directly from the context
                val playerHealth = with(ctx.world) { ctx.playerEntity[io.bennyoe.components.HealthComponent] }

                val damage = shakeAttack?.baseDamage ?: 2f
                playerHealth.takeDamage(damage)

                // Optional: Send a message or play a specific sound here if you want sync with damage
                // ctx.stage.fire(PlaySoundEvent(...))
            }

            if (shakeTimeCounter < SHAKE_DURATION) {
                shakeTimeCounter += ctx.deltaTime
                return
            }
            ctx.changeState(THROWING_PLAYER())
        }

        override fun exit(ctx: MinotaurStateContext) {
            shakeTimeCounter = 0f // Reset global counter
            damageTickTimer = 0f // Reset local counter
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
            ctx.releasePlayer()
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
        private val pickupFrameIndex = 2
        private val releaseFrameIndex = 3

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

            if (!hasSpawnedRock && currentFrame >= pickupFrameIndex) {
                ctx.spawnRock()
                hasSpawnedRock = true
            }

            if (!hasThrownRock && currentFrame >= releaseFrameIndex) {
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
            val playerPos = ctx.getPlayerPosition()
            ctx.setAnimation(MinotaurAnimation.STUNNED)
            ctx.spawnShockwave(playerPos)
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
        var stompAttackApplied = false

        override fun enter(ctx: MinotaurStateContext) {
            ctx.setAnimation(MinotaurAnimation.STOMP_ATTACK, Animation.PlayMode.NORMAL, true)
            ctx.intentionCmp.wantsToJump = true
        }

        override fun update(ctx: MinotaurStateContext) {
            if (ctx.animationComponent.animation.getKeyFrameIndex(ctx.animationComponent.stateTime) == 2 && !stompAttackApplied) {
                ctx.stompAttack()
                stompAttackApplied = true
            }
            if (ctx.animationComponent.isAnimationFinished()) {
                ctx.intentionCmp.wantsToJump = false
                stompAttackApplied = false
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
