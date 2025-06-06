package io.bennyoe.state

import com.badlogic.gdx.ai.fsm.State
import com.badlogic.gdx.ai.msg.Telegram
import com.badlogic.gdx.graphics.g2d.Animation
import io.bennyoe.components.AnimationType
import io.bennyoe.components.AnimationVariant
import io.bennyoe.components.BashComponent
import io.bennyoe.components.HasGroundContact
import io.bennyoe.components.WalkDirection
import kotlin.math.abs

// Constant defining the minimum vertical velocity threshold to detect landing
private const val LANDING_VELOCITY_EPS = 0.1f
private const val DOUBLE_JUMP_FALL_DELAY_DURATION = .1f

@Suppress("ClassName")
sealed class PlayerFSM : State<StateContext> {
    // a delta time to prevent switching from DOUBLE_JUMP to FALL instantly because of the async time-steps (fixed time in physicSystem and frames in
    // FSM) and therefore having for a short time a negative y-velocity
    protected var doubleJumpFallDelay = 0f

    // this is needed to prevent flickering of the death animation
    protected var deathAlreadyEnteredBefore = false

    protected fun shouldIdle(ctx: StateContext) = ctx.inputComponent.direction == WalkDirection.NONE

    protected fun shouldWalk(ctx: StateContext) = ctx.inputComponent.direction != WalkDirection.NONE

    protected fun shouldJump(ctx: StateContext) = ctx.inputComponent.jumpJustPressed

    protected fun hasGroundContact(ctx: StateContext) = with(ctx.world) { ctx.entity has HasGroundContact }

    protected fun shouldFall(ctx: StateContext): Boolean {
        val vy = ctx.physicComponent.body.linearVelocity.y
        // Only treat as "falling" if we are clearly moving downward AND not touching the ground
        return vy < -LANDING_VELOCITY_EPS && !hasGroundContact(ctx)
    }

    protected fun shouldCrouch(ctx: StateContext) = ctx.inputComponent.crouch

    protected fun shouldAttack(ctx: StateContext) = ctx.inputComponent.attackJustPressed

    protected fun shouldAttack2(ctx: StateContext) = ctx.inputComponent.attack2JustPressed

    protected fun shouldAttack3(ctx: StateContext) = ctx.inputComponent.attack3JustPressed

    protected fun shouldBash(ctx: StateContext) = ctx.inputComponent.bashJustPressed

    data object IDLE : PlayerFSM() {
        // TODO after discussing the jump mechanics I maybe need to adjust that JUMP is only possible when the prevState is also IDLE. Because now
        //  it is possible to JUMP & BASH against a wall and the JUMP and DOUBLE_JUMP again

        override fun enter(ctx: StateContext) {
            logger.debug { "Entering IDLE" }
            ctx.setAnimation(AnimationType.IDLE)
        }

        override fun update(ctx: StateContext) {
            when {
                shouldCrouch(ctx) && shouldWalk(ctx) -> ctx.changeState(CROUCH_WALK)
                // because state changes for a fraction while JUMP to IDLE before FALL, also need to check for groundContact
                shouldJump(ctx) && hasGroundContact(ctx) -> ctx.changeState(JUMP)
                shouldCrouch(ctx) -> ctx.changeState(CROUCH_IDLE)
                shouldWalk(ctx) -> ctx.changeState(WALK)
                shouldAttack(ctx) -> ctx.changeState(ATTACK_1)
                shouldBash(ctx) -> ctx.changeState(BASH)
                shouldFall(ctx) -> ctx.changeState(FALL)
            }
        }

        override fun onMessage(
            ctx: StateContext,
            telegram: Telegram,
        ): Boolean {
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
        override fun enter(ctx: StateContext) {
            logger.debug { "Entering WALK" }
            ctx.setAnimation(AnimationType.WALK)
        }

        override fun update(ctx: StateContext) {
            when {
                shouldBash(ctx) -> ctx.changeState(BASH)
                shouldAttack(ctx) -> ctx.changeState(ATTACK_1)
                shouldIdle(ctx) -> ctx.changeState(IDLE)
                // because state changes for a fraction while JUMP to WALK before FALL when pressing walk-key, also need to check for groundContact
                shouldJump(ctx) && hasGroundContact(ctx) -> ctx.changeState(JUMP)
                shouldCrouch(ctx) -> ctx.changeState(CROUCH_WALK)
                shouldFall(ctx) -> ctx.changeState(FALL)
            }
        }
    }

    data object JUMP : PlayerFSM() {
        override fun enter(ctx: StateContext) {
            logger.debug { "Entering JUMP" }
            ctx.jumpComponent.wantsToJump = true
            ctx.inputComponent.jumpJustPressed = false
            ctx.setAnimation(AnimationType.JUMP)
        }

        override fun update(ctx: StateContext) {
            when {
                shouldBash(ctx) -> ctx.changeState(BASH)
                shouldAttack(ctx) -> ctx.changeState(ATTACK_1)
                shouldFall(ctx) -> ctx.changeState(FALL)
                shouldJump(ctx) -> ctx.changeState(DOUBLE_JUMP)
            }
        }
    }

    data object DOUBLE_JUMP : PlayerFSM() {
        override fun enter(ctx: StateContext) {
            logger.debug { "Entering DOUBLE_JUMP" }
            ctx.jumpComponent.wantsToJump = true
            ctx.inputComponent.jumpJustPressed = false
            ctx.setAnimation(AnimationType.JUMP)
            doubleJumpFallDelay = DOUBLE_JUMP_FALL_DELAY_DURATION
        }

        override fun update(ctx: StateContext) {
            if (doubleJumpFallDelay > 0f) {
                doubleJumpFallDelay -= ctx.deltaTime
                return
            }
            when {
                shouldBash(ctx) -> ctx.changeState(BASH)
                shouldAttack(ctx) -> ctx.changeState(ATTACK_1)
                shouldFall(ctx) -> ctx.changeState(FALL)
                hasGroundContact(ctx) -> ctx.changeState(IDLE)
            }
        }
    }

    data object FALL : PlayerFSM() {
        override fun enter(ctx: StateContext) {
            logger.debug { "Entering FALL" }
            ctx.setAnimation(AnimationType.JUMP)
        }

        override fun update(ctx: StateContext) {
            val velY = ctx.physicComponent.body.linearVelocity.y
            when {
                shouldJump(ctx) && ctx.jumpComponent.doubleJumpGraceTimer > 0f && ctx.previousState() == JUMP -> {
                    ctx.changeState(DOUBLE_JUMP)
                    ctx.inputComponent.jumpJustPressed = false
                }

                shouldBash(ctx) -> ctx.changeState(BASH)
                // Land only when we actually touch the ground *and* vertical speed is ~0
                hasGroundContact(ctx) && abs(velY) <= LANDING_VELOCITY_EPS -> ctx.changeState(IDLE)
                // otherwise remain in FALL
                else -> ctx.inputComponent.jumpJustPressed = false
            }
        }
    }

    data object CROUCH_IDLE : PlayerFSM() {
        override fun enter(ctx: StateContext) {
            logger.debug { "Entering CROUCH_IDLE" }
            ctx.setAnimation(AnimationType.CROUCH_IDLE)
        }

        override fun update(ctx: StateContext) {
            when {
                shouldWalk(ctx) && shouldCrouch(ctx) -> ctx.changeState(CROUCH_WALK)
                shouldIdle(ctx) && !shouldCrouch(ctx) -> ctx.changeState(IDLE)
                shouldWalk(ctx) && !shouldCrouch(ctx) -> ctx.changeState(WALK)
            }
        }
    }

    data object CROUCH_WALK : PlayerFSM() {
        override fun enter(ctx: StateContext) {
            logger.debug { "Entering CROUCH_WALK" }
            ctx.setAnimation(AnimationType.CROUCH_WALK)
        }

        override fun update(ctx: StateContext) {
            when {
                !shouldCrouch(ctx) && shouldIdle(ctx) -> ctx.changeState(IDLE)
                !shouldCrouch(ctx) && shouldWalk(ctx) -> ctx.changeState(WALK)
                shouldCrouch(ctx) && shouldIdle(ctx) -> ctx.changeState(CROUCH_IDLE)
            }
        }
    }

    data object ATTACK_1 : PlayerFSM() {
        override fun enter(ctx: StateContext) {
            logger.debug { "Entering ATTACK_1" }
            ctx.inputComponent.attackJustPressed = false
            ctx.setAnimation(AnimationType.ATTACK)
            ctx.attackComponent.applyAttack = true
        }

        override fun update(ctx: StateContext) {
            if (shouldAttack(ctx)) ctx.inputComponent.attack2JustPressed = true
            if (ctx.animationComponent.isAnimationFinished()) {
                when {
                    shouldAttack2(ctx) -> ctx.changeState(ATTACK_2)
                    shouldFall(ctx) -> ctx.changeState(FALL)
                    else -> ctx.changeState(IDLE)
                }
            }
        }
    }

    data object ATTACK_2 : PlayerFSM() {
        override fun enter(ctx: StateContext) {
            logger.debug { "Entering ATTACK_2" }
            ctx.inputComponent.attack2JustPressed = false
            ctx.setAnimation(AnimationType.ATTACK, variant = AnimationVariant.SECOND)
            ctx.attackComponent.applyAttack = true
        }

        override fun update(ctx: StateContext) {
            if (shouldAttack(ctx)) ctx.inputComponent.attack3JustPressed = true
            if (ctx.animationComponent.isAnimationFinished()) {
                when {
                    shouldAttack3(ctx) -> ctx.changeState(ATTACK_3)
                    shouldFall(ctx) -> ctx.changeState(FALL)
                    else -> ctx.changeState(IDLE)
                }
            }
        }
    }

    data object ATTACK_3 : PlayerFSM() {
        override fun enter(ctx: StateContext) {
            logger.debug { "Entering ATTACK_3" }
            ctx.inputComponent.attack3JustPressed = false
            ctx.setAnimation(AnimationType.ATTACK, variant = AnimationVariant.THIRD)
            ctx.attackComponent.applyAttack = true
        }

        override fun update(ctx: StateContext) {
            if (ctx.animationComponent.isAnimationFinished()) {
                when {
                    shouldFall(ctx) -> ctx.changeState(FALL)
                    else -> ctx.changeState(IDLE)
                }
            }
        }
    }

    data object BASH : PlayerFSM() {
        override fun enter(ctx: StateContext) {
            logger.debug { "Entering BASH" }
            ctx.add(BashComponent())
            ctx.setAnimation(AnimationType.BASH)
        }

        override fun update(ctx: StateContext) {
            if (ctx.animationComponent.isAnimationFinished()) {
                when {
                    shouldFall(ctx) -> ctx.changeState(FALL)
                    else -> ctx.changeState(IDLE)
                }
            }
        }
    }

    data object DEATH : PlayerFSM() {
        override fun enter(ctx: StateContext) {
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
            ctx: StateContext,
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
        override fun enter(ctx: StateContext) {
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

        override fun update(ctx: StateContext) {
            if (ctx.animationComponent.isAnimationFinished()) {
                ctx.moveComponent.lockMovement = false
                ctx.stateComponent.stateMachine.globalState = GlobalState.CHECK_ALIVE
                ctx.changeState(IDLE)
            }
        }
    }

    override fun enter(ctx: StateContext) = Unit

    override fun update(ctx: StateContext) = Unit

    override fun exit(ctx: StateContext) = Unit

    override fun onMessage(
        ctx: StateContext,
        telegram: Telegram,
    ) = false

    companion object {
        val logger = ktx.log.logger<PlayerFSM>()
    }
}
