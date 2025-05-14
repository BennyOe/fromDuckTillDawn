package io.bennyoe.ai

import com.badlogic.gdx.ai.fsm.State
import com.badlogic.gdx.ai.msg.Telegram
import io.bennyoe.components.AnimationType
import ktx.log.logger

interface GlobalState : State<StateContext> {
    val aniType: AnimationType

    override fun enter(ctx: StateContext) = ctx.setAnimation(aniType)

    override fun update(ctx: StateContext) = Unit

    override fun exit(ctx: StateContext) = Unit

    override fun onMessage(
        ctx: StateContext,
        telegram: Telegram,
    ): Boolean = true
}

enum class DefaultState(
    override val aniType: AnimationType,
) : GlobalState {
    NONE(AnimationType.IDLE) {
        override fun update(ctx: StateContext) {
            val health = ctx.healthComponent
            if (health.isDead) {
                ctx.aiComponent.stateMachine.globalState = DEATH
                ctx.aiComponent.stateMachine.changeState(PlayerFSM.SHOW_DEATH)
                return
            }
        }

        override fun onMessage(
            ctx: StateContext,
            telegram: Telegram,
        ): Boolean {
            if (telegram.message == FsmMessageTypes.KILL.ordinal && telegram.extraInfo == true) {
                ctx.healthComponent.current = 0
            }
            return true
        }
    },
    DEATH(AnimationType.BASH) {
        private val logger = logger<GlobalState>()

        override fun update(ctx: StateContext) {
        }
    },
}
