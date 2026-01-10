package io.bennyoe.state.spector

import com.badlogic.gdx.ai.msg.Telegram
import io.bennyoe.state.FsmMessageTypes

class SpectorCheckAliveState : SpectorFSM() {
    override fun update(ctx: SpectorStateContext) {
        if (ctx.healthComponent.isDead) {
            ctx.changeState(DEATH())
            return
        }
    }

    override fun onMessage(
        ctx: SpectorStateContext,
        telegram: Telegram,
    ): Boolean {
        if (telegram.message == FsmMessageTypes.KILL.ordinal && telegram.extraInfo == true) {
            ctx.healthComponent.current = 0f
        }
        return true
    }
}
