package io.bennyoe.ai.actions

import io.bennyoe.ai.core.AbstractAction
import io.bennyoe.components.WalkDirection
import ktx.log.logger

class Patrol : AbstractAction() {
    override fun enter() {
        logger.debug { "Patrol Enter" }
        ctx.stopAttack()
        ctx.lastTaskName = this.javaClass.simpleName
        ctx.intentionCmp.walkDirection = WalkDirection.RIGHT
    }

    override fun onExecute(): Status {
        ctx.patrol()
        return Status.RUNNING
    }

    companion object {
        val logger = logger<Patrol>()
    }
}
