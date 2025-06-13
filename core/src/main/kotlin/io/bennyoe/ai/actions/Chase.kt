package io.bennyoe.ai.actions

import io.bennyoe.ai.core.AbstractAction
import io.bennyoe.components.WalkDirection
import ktx.log.logger

class Chase : AbstractAction() {
    override fun enter() {
        logger.debug { "Chase Enter" }
        ctx.lastTaskName = this.javaClass.simpleName
        ctx.intentionCmp.wantsToChase = true
        ctx.stopAttack()
        ctx.intentionCmp.walkDirection = WalkDirection.NONE
    }

    override fun onExecute(): Status {
        ctx.chasePlayer()
        return Status.RUNNING
    }

    override fun exit() {
        ctx.nearestPlatformLedge = null
        ctx.intentionCmp.wantsToChase = false
    }

    companion object {
        val logger = logger<Chase>()
    }
}
