package io.bennyoe.ai.actions.spector

import com.badlogic.gdx.Gdx
import io.bennyoe.ai.blackboards.SpectorContext
import io.bennyoe.ai.core.AbstractAction
import io.bennyoe.components.GameMood

class SpectorChase : AbstractAction<SpectorContext>() {
    private var timer = 0f

    override fun enter() {
        timer = 0f
        ctx.moveCmp.maxWalkSpeed = 8f
        ctx.stopAttack()
        ctx.currentMood = GameMood.CHASE
    }

    override fun onExecute(): Status {
        timer += Gdx.graphics.deltaTime

        return if (timer >= 3f) {
            Status.SUCCEEDED
        } else {
            ctx.chasePlayer(ctx.world)
            Status.RUNNING
        }
    }

    override fun exit() {
        ctx.moveCmp.maxWalkSpeed = ctx.initialWalkSpeed
    }
}
