package io.bennyoe.ai.actions.minotaur

import io.bennyoe.ai.blackboards.MinotaurContext
import io.bennyoe.ai.core.AbstractAction
import io.bennyoe.components.GameMood
import ktx.log.logger

class MinotaurThrowAttack : AbstractAction<MinotaurContext>() {
    override fun enter() {
        ctx.stopMovement()
        logger.debug { "Throw Attack Enter" }
        ctx.lastTaskName = this.javaClass.simpleName
        ctx.currentMood = GameMood.CHASE
        ctx.startThrowAttack()
    }

    override fun onExecute(): Status {
        if (ctx.isAnimationFinished()) {
            return Status.SUCCEEDED
        }
        return Status.RUNNING
    }

    companion object {
        val logger = logger<MinotaurThrowAttack>()
    }
}
