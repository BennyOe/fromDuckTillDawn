package io.bennyoe.ai.actions.spector

import com.badlogic.gdx.Gdx
import io.bennyoe.ai.blackboards.SpectorContext
import io.bennyoe.ai.core.AbstractAction
import io.bennyoe.components.GameMood

class SpectorSearch : AbstractAction<SpectorContext>() {
    private var timer = 0f

    override fun enter() {
        timer = 0f
        ctx.stopAttack()
        ctx.currentMood = GameMood.NORMAL
    }

    override fun onExecute(): Status {
        timer += Gdx.graphics.deltaTime

        return if (timer >= 3f) {
            Status.SUCCEEDED
        } else {
            ctx.stopMovement()
            Status.RUNNING
        }
    }
}
