package io.bennyoe.state.minotaur

import com.badlogic.gdx.ai.msg.Telegram
import io.bennyoe.state.FsmMessageTypes

class MinotaurCheckAliveState : MinotaurFSM() {
    override fun update(ctx: MinotaurStateContext) {
        if (ctx.healthComponent.isDead) {
            ctx.changeState(DEATH())
            return
        }
    }

    override fun onMessage(
        ctx: MinotaurStateContext,
        telegram: Telegram,
    ): Boolean {
        if (telegram.message == FsmMessageTypes.KILL.ordinal && telegram.extraInfo == true) {
            ctx.healthComponent.current = 0f
        }
        return true
    }
}
