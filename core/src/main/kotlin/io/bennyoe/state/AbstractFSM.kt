package io.bennyoe.state

import com.badlogic.gdx.ai.fsm.State
import com.badlogic.gdx.ai.msg.Telegram
import io.bennyoe.components.HasGroundContact
import io.bennyoe.components.HasWaterContact
import io.bennyoe.components.IsDiving

// Constant defining the minimum vertical velocity threshold to detect landing
const val LANDING_VELOCITY_EPS = 0.1f

abstract class AbstractFSM<C : AbstractStateContext<C>> : State<C> {
    protected fun hasGroundContact(ctx: AbstractStateContext<C>) = with(ctx.world) { ctx.entity has HasGroundContact }

    protected fun hasWaterContact(ctx: AbstractStateContext<C>) = with(ctx.world) { ctx.entity has HasWaterContact }

    protected fun isDiving(ctx: AbstractStateContext<C>) = with(ctx.world) { ctx.entity has IsDiving }

    protected fun isFalling(ctx: AbstractStateContext<C>): Boolean {
        val vy = ctx.physicComponent.body.linearVelocity.y
        return vy < -LANDING_VELOCITY_EPS && !hasGroundContact(ctx)
    }

    override fun enter(ctx: C) = Unit

    override fun update(ctx: C) = Unit

    override fun exit(ctx: C) = Unit

    override fun onMessage(
        ctx: C,
        telegram: Telegram,
    ): Boolean = false
}
