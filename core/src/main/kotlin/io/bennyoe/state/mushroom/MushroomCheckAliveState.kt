package io.bennyoe.state.mushroom

import com.badlogic.gdx.ai.msg.Telegram
import io.bennyoe.state.FsmMessageTypes

object MushroomCheckAliveState : MushroomFSM() {
    override fun update(ctx: MushroomStateContext) {
        if (ctx.healthComponent.isDead) {
            ctx.changeState(DEATH)
            return
        }
    }

    override fun onMessage(
        ctx: MushroomStateContext,
        telegram: Telegram,
    ): Boolean {
        if (telegram.message == FsmMessageTypes.KILL.ordinal && telegram.extraInfo == true) {
            ctx.healthComponent.current = 0f
        }
        return true
    }
}
