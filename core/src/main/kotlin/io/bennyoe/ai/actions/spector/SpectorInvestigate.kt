package io.bennyoe.ai.actions.spector

import com.badlogic.gdx.Gdx
import io.bennyoe.ai.blackboards.SpectorContext
import io.bennyoe.ai.core.AbstractAction
import ktx.log.logger

const val INVESTIGATION_DURATION = 2f

class SpectorInvestigate : AbstractAction<SpectorContext>() {
    var timer = 0f

    override fun enter() {
        timer = 0f
        ctx.lastTaskName = this.javaClass.simpleName
        ctx.idle()
    }

    override fun onExecute(): Status {
        timer += Gdx.graphics.deltaTime

        return if (timer >= INVESTIGATION_DURATION) {
            Status.SUCCEEDED
        } else {
            ctx.stopMovement()
            Status.RUNNING
        }
    }

    override fun exit() {
    }

    companion object {
        val logger = logger<SpectorCancelInvestigation>()
    }
}
