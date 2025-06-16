package io.bennyoe.ai.actions

import io.bennyoe.ai.core.AbstractAction
import ktx.log.logger

class Attack : AbstractAction() {
    override fun enter() {
        ctx.stopMovement()
        logger.debug { "Attack Enter" }
        ctx.lastTaskName = this.javaClass.simpleName
        ctx.startAttack()
    }

    override fun onExecute(): Status {
        if (ctx.isAnimationFinished()) {
            return Status.SUCCEEDED
        }
        return Status.RUNNING
    }

    companion object {
        val logger = logger<Attack>()
    }
}
