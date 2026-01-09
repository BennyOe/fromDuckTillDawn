package io.bennyoe.ai.actions.spector

import com.badlogic.gdx.Gdx
import io.bennyoe.ai.blackboards.SpectorContext
import io.bennyoe.ai.core.AbstractAction
import io.bennyoe.components.GameMood
import ktx.log.logger

class SpectorInvestigate : AbstractAction<SpectorContext>() {
    override fun enter() {
        ctx.lastTaskName = this.javaClass.simpleName
        ctx.currentMood = GameMood.NORMAL
        ctx.idle()
        ctx.investigationIsFinished = false
    }

    override fun onExecute(): Status = Status.RUNNING

    override fun exit() {
        ctx.investigationIsFinished = true
    }

    companion object {
        val logger = logger<SpectorInvestigate>()
    }
}
