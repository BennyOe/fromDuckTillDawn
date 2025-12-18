package io.bennyoe.ai.actions.minotaur

import io.bennyoe.ai.blackboards.MinotaurContext
import io.bennyoe.ai.core.AbstractAction
import io.bennyoe.components.GameMood
import ktx.log.logger

class MinotaurStompAttack : AbstractAction<MinotaurContext>() {
    override fun enter() {
        ctx.stopMovement()
        logger.debug { "Stomp Attack Enter" }
        ctx.lastTaskName = this.javaClass.simpleName
        ctx.currentMood = GameMood.CHASE
        ctx.startStompAttack()
    }

    override fun onExecute(): Status {
        if (ctx.isAnimationFinished()) {
            return Status.SUCCEEDED
        }
        return Status.RUNNING
    }

    companion object {
        val logger = logger<MinotaurStompAttack>()
    }
}
