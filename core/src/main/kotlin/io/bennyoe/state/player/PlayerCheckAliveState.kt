package io.bennyoe.state.player

import com.badlogic.gdx.ai.msg.Telegram
import io.bennyoe.state.AbstractStateContext
import io.bennyoe.state.FsmMessageTypes

object PlayerCheckAliveState : PlayerFSM() {
    override fun update(ctx: AbstractStateContext) {
        if (ctx !is PlayerStateContext) return
        if (ctx.healthComponent.isDead) {
            ctx.stateComponent.changeState(DEATH)
            return
        }
    }

    override fun onMessage(
        ctx: AbstractStateContext,
        telegram: Telegram,
    ): Boolean {
        if (ctx !is PlayerStateContext) return false
        if (telegram.message == FsmMessageTypes.KILL.ordinal && telegram.extraInfo == true) {
            ctx.healthComponent.current = 0f
        }
        return true
    }
}
