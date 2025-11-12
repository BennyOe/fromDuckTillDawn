package io.bennyoe.ai.actions.minotaur

import io.bennyoe.ai.blackboards.MinotaurContext
import io.bennyoe.ai.core.AbstractAction
import io.bennyoe.components.GameMood
import io.bennyoe.components.WalkDirection
import io.bennyoe.state.minotaur.MinotaurFSM
import ktx.log.logger

class MinotaurScream : AbstractAction<MinotaurContext>() {
    private var hasEnteredScream = false

    override fun enter() {
        logger.debug { "Scream Enter" }
        ctx.lastTaskName = this.javaClass.simpleName
        ctx.currentMood = GameMood.CHASE
        ctx.intentionCmp.wantsToScream = true
        ctx.stopAttack()
        ctx.intentionCmp.walkDirection = WalkDirection.NONE
    }

    override fun onExecute(): Status {
        // CHANGED: Check for interruption by HIT (or other unexpected states)
        // If we are in a state that isn't leading to or executing scream, fail this task.
        if (ctx.fsmStateIsNot<MinotaurFSM.IDLE>() &&
            ctx.fsmStateIsNot<MinotaurFSM.WALK>() &&
            ctx.fsmStateIsNot<MinotaurFSM.SCREAM>()
        ) {
            // We were interrupted by a state like HIT
            return Status.FAILED
        }

        if (!hasEnteredScream) {
            // Phase 1: Wait for FSM to enter SCREAM
            if (ctx.fsmStateIs<MinotaurFSM.SCREAM>()) {
                hasEnteredScream = true
            }
            // We are either in IDLE/WALK (waiting) or have just entered SCREAM
            return Status.RUNNING
        }

        // Phase 2: We are in SCREAM and waiting for it to finish
        if (ctx.fsmStateIs<MinotaurFSM.SCREAM>()) {
            return Status.RUNNING
        }

        // Phase 3: SCREAM has been exited.
        // If we exited SCREAM and are now in IDLE, it was a normal success.
        // If we were interrupted by HIT, the check at the top would have caught it
        // on the *next* tick, but we can also check here.
        if (ctx.fsmStateIs<MinotaurFSM.IDLE>()) {
            return Status.SUCCEEDED
        }

        // Exited scream but not to IDLE (e.g., HIT state)
        return Status.FAILED
    }

    override fun exit() {
        ctx.intentionCmp.wantsToScream = false
        hasEnteredScream = false
        logger.debug { "Scream EXIT" }
    }

    companion object {
        val logger = logger<MinotaurScream>()
    }
}
