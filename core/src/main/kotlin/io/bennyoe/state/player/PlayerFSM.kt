package io.bennyoe.state.player

import com.badlogic.gdx.ai.msg.Telegram
import com.badlogic.gdx.graphics.g2d.Animation
import io.bennyoe.components.AnimationType
import io.bennyoe.components.AnimationVariant
import io.bennyoe.components.BashComponent
import io.bennyoe.state.AbstractFSM
import io.bennyoe.state.FsmMessageTypes
import io.bennyoe.state.LANDING_VELOCITY_EPS
import ktx.log.logger
import kotlin.math.abs

private const val DOUBLE_JUMP_FALL_DELAY_DURATION = .1f

@Suppress("ClassName")
sealed class PlayerFSM : AbstractFSM<PlayerStateContext>() {
    // a delta time to prevent switching from DOUBLE_JUMP to FALL instantly because of the async time-steps (fixed time in physicSystem and frames in
    // FSM) and therefore having for a short time a negative y-velocity
    protected var doubleJumpFallDelay = 0f

    data object IDLE : PlayerFSM() {
        // TODO after discussing the jump mechanics I maybe need to adjust that JUMP is only possible when the prevState is also IDLE. Because now
        //  it is possible to JUMP & BASH against a wall and the JUMP and DOUBLE_JUMP again

        override fun enter(ctx: PlayerStateContext) {
            logger.debug { "Entering IDLE" }
            ctx.setAnimation(AnimationType.IDLE)
        }

        override fun update(ctx: PlayerStateContext) {
            when {
                ctx.wantsToCrouch && ctx.wantsToWalk -> ctx.changeState(CROUCH_WALK)
                // because state changes for a fraction while JUMP to IDLE before FALL, also need to check for groundContact
                ctx.wantsToJump && hasGroundContact(ctx) -> ctx.changeState(JUMP)
                ctx.jumpComponent.jumpFromBuffer -> ctx.changeState(JUMP)
                ctx.wantsToCrouch -> ctx.changeState(CROUCH_IDLE)
                ctx.wantsToWalk -> ctx.changeState(WALK)
                ctx.wantsToAttack -> ctx.changeState(ATTACK_1)
                ctx.wantsToBash -> ctx.changeState(BASH)
                isFalling(ctx) -> ctx.changeState(FALL)
            }
        }

        override fun onMessage(
            ctx: PlayerStateContext,
            telegram: Telegram,
        ): Boolean {
            super.onMessage(ctx, telegram)
            if (telegram.message == FsmMessageTypes.HEAL.ordinal && telegram.extraInfo == true) {
                logger.debug { "MESSAGE WITH HEAL RECEIVED INSTANTLY" }
                return true
            } else if (telegram.message == FsmMessageTypes.ATTACK.ordinal && telegram.extraInfo == true) {
                logger.debug { "MESSAGE WITH ATTACK RECEIVED AFTER A DELAY" }
                return true
            }
            return false
        }
    }

    data object WALK : PlayerFSM() {
        override fun enter(ctx: PlayerStateContext) {
            logger.debug { "Entering WALK" }
            ctx.setAnimation(AnimationType.WALK)
        }

        override fun update(ctx: PlayerStateContext) {
            when {
                ctx.wantsToBash -> ctx.changeState(BASH)
                ctx.wantsToAttack -> ctx.changeState(ATTACK_1)
                ctx.wantsToIdle -> ctx.changeState(IDLE)
                // because state changes for a fraction while JUMP to WALK before FALL when pressing walk-key, also need to check for groundContact
                ctx.wantsToJump && hasGroundContact(ctx) -> ctx.changeState(JUMP)
                ctx.wantsToCrouch -> ctx.changeState(CROUCH_WALK)
                isFalling(ctx) -> ctx.changeState(FALL)
            }
        }

        override fun onMessage(
            ctx: PlayerStateContext,
            telegram: Telegram,
        ): Boolean = super.onMessage(ctx, telegram)
    }

    data object JUMP : PlayerFSM() {
        override fun enter(ctx: PlayerStateContext) {
            logger.debug { "Entering JUMP" }
            ctx.jumpComponent.wantsToJump = true
            ctx.intentionCmp.wantsToJump = false
            ctx.jumpComponent.jumpFromBuffer = false
            ctx.setAnimation(AnimationType.JUMP)
        }

        override fun update(ctx: PlayerStateContext) {
            when {
                ctx.wantsToBash -> ctx.changeState(BASH)
                ctx.wantsToAttack -> ctx.changeState(ATTACK_1)
                isFalling(ctx) -> ctx.changeState(FALL)
                ctx.wantsToJump -> ctx.changeState(DOUBLE_JUMP)
            }
        }

        override fun onMessage(
            ctx: PlayerStateContext,
            telegram: Telegram,
        ): Boolean = super.onMessage(ctx, telegram)
    }

    data object DOUBLE_JUMP : PlayerFSM() {
        override fun enter(ctx: PlayerStateContext) {
            logger.debug { "Entering DOUBLE_JUMP" }
            ctx.jumpComponent.wantsToJump = true
            ctx.intentionCmp.wantsToJump = false
            ctx.jumpComponent.jumpFromBuffer = false
            ctx.setAnimation(AnimationType.JUMP)
            doubleJumpFallDelay = DOUBLE_JUMP_FALL_DELAY_DURATION
        }

        override fun update(ctx: PlayerStateContext) {
            if (doubleJumpFallDelay > 0f) {
                doubleJumpFallDelay -= ctx.deltaTime
                return
            }
            when {
                ctx.wantsToBash -> ctx.changeState(BASH)
                ctx.wantsToAttack -> ctx.changeState(ATTACK_1)
                isFalling(ctx) -> ctx.changeState(FALL)
                hasGroundContact(ctx) -> ctx.changeState(IDLE)
            }
        }

        override fun onMessage(
            ctx: PlayerStateContext,
            telegram: Telegram,
        ): Boolean = super.onMessage(ctx, telegram)
    }

    data object FALL : PlayerFSM() {
        override fun enter(ctx: PlayerStateContext) {
            logger.debug { "Entering FALL" }
            ctx.setAnimation(AnimationType.JUMP)
        }

        override fun update(ctx: PlayerStateContext) {
            val velY = ctx.physicComponent.body.linearVelocity.y
            when {
                ctx.wantsToJump && ctx.jumpComponent.doubleJumpGraceTimer > 0f && ctx.previousState() == JUMP -> {
                    ctx.changeState(DOUBLE_JUMP)
                    ctx.intentionCmp.wantsToJump = false
                }

                ctx.wantsToBash -> ctx.changeState(BASH)
                // Land only when we actually touch the ground *and* vertical speed is ~0
                hasGroundContact(ctx) && abs(velY) <= LANDING_VELOCITY_EPS -> ctx.changeState(IDLE)
                // otherwise remain in FALL
                else -> ctx.intentionCmp.wantsToJump = false
            }
        }

        override fun onMessage(
            ctx: PlayerStateContext,
            telegram: Telegram,
        ): Boolean = super.onMessage(ctx, telegram)
    }

    data object CROUCH_IDLE : PlayerFSM() {
        override fun enter(ctx: PlayerStateContext) {
            logger.debug { "Entering CROUCH_IDLE" }
            ctx.setAnimation(AnimationType.CROUCH_IDLE)
        }

        override fun update(ctx: PlayerStateContext) {
            when {
                ctx.wantsToWalk && ctx.wantsToCrouch -> ctx.changeState(CROUCH_WALK)
                ctx.wantsToIdle && !ctx.wantsToCrouch -> ctx.changeState(IDLE)
                ctx.wantsToWalk && !ctx.wantsToCrouch -> ctx.changeState(WALK)
            }
        }

        override fun onMessage(
            ctx: PlayerStateContext,
            telegram: Telegram,
        ): Boolean = super.onMessage(ctx, telegram)
    }

    data object CROUCH_WALK : PlayerFSM() {
        override fun enter(ctx: PlayerStateContext) {
            logger.debug { "Entering CROUCH_WALK" }
            ctx.setAnimation(AnimationType.CROUCH_WALK)
        }

        override fun update(ctx: PlayerStateContext) {
            when {
                !ctx.wantsToCrouch && ctx.wantsToIdle -> ctx.changeState(IDLE)
                !ctx.wantsToCrouch && ctx.wantsToWalk -> ctx.changeState(WALK)
                ctx.wantsToCrouch && ctx.wantsToIdle -> ctx.changeState(CROUCH_IDLE)
            }
        }

        override fun onMessage(
            ctx: PlayerStateContext,
            telegram: Telegram,
        ): Boolean = super.onMessage(ctx, telegram)
    }

    data object ATTACK_1 : PlayerFSM() {
        override fun enter(ctx: PlayerStateContext) {
            logger.debug { "Entering ATTACK_1" }
            ctx.intentionCmp.wantsToAttack = false
            ctx.setAnimation(AnimationType.ATTACK)
            ctx.attackComponent.applyAttack = true
        }

        override fun update(ctx: PlayerStateContext) {
            if (ctx.wantsToAttack) ctx.intentionCmp.wantsToAttack2 = true
            if (ctx.animationComponent.isAnimationFinished()) {
                when {
                    ctx.wantsToAttack2 -> ctx.changeState(ATTACK_2)
                    isFalling(ctx) -> ctx.changeState(FALL)
                    else -> ctx.changeState(IDLE)
                }
            }
        }

        override fun onMessage(
            ctx: PlayerStateContext,
            telegram: Telegram,
        ): Boolean = super.onMessage(ctx, telegram)
    }

    data object ATTACK_2 : PlayerFSM() {
        override fun enter(ctx: PlayerStateContext) {
            logger.debug { "Entering ATTACK_2" }
            ctx.intentionCmp.wantsToAttack2 = false
            ctx.setAnimation(AnimationType.ATTACK, variant = AnimationVariant.SECOND)
            ctx.attackComponent.applyAttack = true
        }

        override fun update(ctx: PlayerStateContext) {
            if (ctx.wantsToAttack) ctx.intentionCmp.wantsToAttack3 = true
            if (ctx.animationComponent.isAnimationFinished()) {
                when {
                    ctx.wantsToAttack3 -> ctx.changeState(ATTACK_3)
                    isFalling(ctx) -> ctx.changeState(FALL)
                    else -> ctx.changeState(IDLE)
                }
            }
        }

        override fun onMessage(
            ctx: PlayerStateContext,
            telegram: Telegram,
        ): Boolean = super.onMessage(ctx, telegram)
    }

    data object ATTACK_3 : PlayerFSM() {
        override fun enter(ctx: PlayerStateContext) {
            logger.debug { "Entering ATTACK_3" }
            ctx.intentionCmp.wantsToAttack3 = false
            ctx.setAnimation(AnimationType.ATTACK, variant = AnimationVariant.THIRD)
            ctx.attackComponent.applyAttack = true
        }

        override fun update(ctx: PlayerStateContext) {
            if (ctx.animationComponent.isAnimationFinished()) {
                when {
                    isFalling(ctx) -> ctx.changeState(FALL)
                    else -> ctx.changeState(IDLE)
                }
            }
        }

        override fun onMessage(
            ctx: PlayerStateContext,
            telegram: Telegram,
        ): Boolean = super.onMessage(ctx, telegram)
    }

    data object BASH : PlayerFSM() {
        override fun enter(ctx: PlayerStateContext) {
            logger.debug { "Entering BASH" }
            ctx.add(BashComponent())
            ctx.setAnimation(AnimationType.BASH)
            ctx.intentionCmp.wantsToBash = false
        }

        override fun update(ctx: PlayerStateContext) {
            if (ctx.animationComponent.isAnimationFinished()) {
                when {
                    isFalling(ctx) -> ctx.changeState(FALL)
                    else -> ctx.changeState(IDLE)
                }
            }
        }

        override fun onMessage(
            ctx: PlayerStateContext,
            telegram: Telegram,
        ): Boolean = super.onMessage(ctx, telegram)
    }

    data object HIT : PlayerFSM() {
        override fun enter(ctx: PlayerStateContext) {
            logger.debug { "Entering HIT" }
            ctx.setAnimation(AnimationType.HIT, resetStateTime = true)
            ctx.healthComponent.takenDamage = 0f
        }

        override fun update(ctx: PlayerStateContext) {
            if (ctx.animationComponent.isAnimationFinished()) {
                ctx.changeState(IDLE)
            }
        }
    }

    data object DEATH : PlayerFSM() {
        override fun enter(ctx: PlayerStateContext) {
            logger.debug { "Entering DEATH" }
            logger.debug { " $deathAlreadyEnteredBefore" }
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
            deathAlreadyEnteredBefore = true
        }

        override fun onMessage(
            ctx: PlayerStateContext,
            telegram: Telegram,
        ): Boolean {
            if (telegram.message == FsmMessageTypes.KILL.ordinal && telegram.extraInfo == true) {
                ctx.changeState(RESURRECT)
                return true
            }
            return false
        }
    }

    data object RESURRECT : PlayerFSM() {
        override fun enter(ctx: PlayerStateContext) {
            logger.debug { "Entering RESURRECT" }
            ctx.setAnimation(
                AnimationType.DYING,
                Animation.PlayMode.REVERSED,
                AnimationVariant.FIRST,
                resetStateTime = true,
                isReversed = true,
            )
            ctx.healthComponent.resetHealth()
        }

        override fun update(ctx: PlayerStateContext) {
            if (ctx.animationComponent.isAnimationFinished()) {
                ctx.moveComponent.lockMovement = false
                ctx.setGlobalState(PlayerCheckAliveState)
                ctx.changeState(IDLE)
            }
        }
    }

    override fun enter(ctx: PlayerStateContext) = Unit

    override fun update(ctx: PlayerStateContext) = Unit

    override fun exit(ctx: PlayerStateContext) = Unit

    override fun onMessage(
        ctx: PlayerStateContext,
        telegram: Telegram,
    ): Boolean {
        if (telegram.message == FsmMessageTypes.HIT.ordinal) {
            ctx.changeState(HIT)
            return true
        }
        return false
    }

    companion object {
        val logger = logger<PlayerFSM>()
    }
}
