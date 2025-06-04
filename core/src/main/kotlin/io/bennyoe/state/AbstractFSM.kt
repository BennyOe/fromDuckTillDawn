package io.bennyoe.state

import com.badlogic.gdx.ai.fsm.State
import com.badlogic.gdx.ai.msg.Telegram
import io.bennyoe.components.HasGroundContact

// Constant defining the minimum vertical velocity threshold to detect landing
const val LANDING_VELOCITY_EPS = 0.1f

abstract class AbstractFSM : State<AbstractStateContext> {
    protected fun hasGroundContact(ctx: AbstractStateContext) = with(ctx.world) { ctx.entity has HasGroundContact }

    protected fun isFalling(ctx: AbstractStateContext): Boolean {
        val vy = ctx.physicComponent.body.linearVelocity.y
        return vy < -LANDING_VELOCITY_EPS && !hasGroundContact(ctx)
    }

    override fun enter(ctx: AbstractStateContext) = Unit

    override fun update(ctx: AbstractStateContext) = Unit

    override fun exit(ctx: AbstractStateContext) = Unit

    override fun onMessage(
        ctx: AbstractStateContext,
        telegram: Telegram,
    ): Boolean = false
}
