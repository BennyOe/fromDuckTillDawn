package io.bennyoe.state.player

import com.badlogic.gdx.ai.msg.Telegram
import io.bennyoe.state.FsmMessageTypes

object PlayerCheckAliveState : PlayerFSM() {
    override fun update(ctx: PlayerStateContext) {
        if (ctx.healthComponent.isDead) {
            ctx.changeState(DEATH)
            return
        }
    }

    override fun onMessage(
        ctx: PlayerStateContext,
        telegram: Telegram,
    ): Boolean {
        if (telegram.message == FsmMessageTypes.KILL.ordinal && telegram.extraInfo == true) {
            ctx.healthComponent.current = 0f
        }
        return true
    }
}
