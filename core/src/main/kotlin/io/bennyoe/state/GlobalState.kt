package io.bennyoe.state

import com.badlogic.gdx.ai.fsm.State
import com.badlogic.gdx.ai.msg.Telegram

enum class GlobalState : State<StateContext> {
    CHECK_ALIVE() {
        override fun enter(entity: StateContext?) = Unit

        override fun update(ctx: StateContext) {
            if (ctx.healthComponent.isDead) {
                ctx.stateComponent.changeState(PlayerFSM.DEATH)
                return
            }
        }

        override fun exit(entity: StateContext?) = Unit

        override fun onMessage(
            ctx: StateContext,
            telegram: Telegram,
        ): Boolean {
            if (telegram.message == FsmMessageTypes.KILL.ordinal && telegram.extraInfo == true) {
                ctx.healthComponent.current = 0f
            }
            return true
        }
    },
}
