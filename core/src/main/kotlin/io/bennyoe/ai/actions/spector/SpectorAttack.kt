package io.bennyoe.ai.actions.spector

import io.bennyoe.ai.blackboards.MushroomContext
import io.bennyoe.ai.blackboards.SpectorContext
import io.bennyoe.ai.core.AbstractAction
import io.bennyoe.components.GameMood
import ktx.log.logger

class SpectorAttack : AbstractAction<SpectorContext>() {
    override fun enter() {
        ctx.stopMovement()
        logger.debug { "Attack Enter" }
        ctx.lastTaskName = this.javaClass.simpleName
        ctx.currentMood = GameMood.CHASE
        ctx.startAttack()
    }

    override fun onExecute(): Status {
        if (ctx.isAnimationFinished()) {
            return Status.SUCCEEDED
        }
        return Status.RUNNING
    }

    companion object {
        val logger = logger<SpectorAttack>()
    }
}
