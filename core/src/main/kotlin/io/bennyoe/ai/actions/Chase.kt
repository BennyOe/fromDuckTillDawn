package io.bennyoe.ai.actions

import com.badlogic.gdx.ai.GdxAI
import io.bennyoe.ai.core.AbstractAction
import io.bennyoe.components.WalkDirection
import ktx.log.logger
import kotlin.math.abs

const val DURATION_TIMER = .5f
const val EPS = 1.0f

class Chase : AbstractAction() {
    private var currentDuration = DURATION_TIMER
    private var xPosition = 0f

    override fun enter() {
        logger.debug { "Chase Enter" }
        ctx.lastTaskName = this.javaClass.simpleName
        ctx.intentionCmp.wantsToChase = true
        ctx.stopAttack()
        ctx.intentionCmp.walkDirection = WalkDirection.NONE
    }

    override fun onExecute(): Status {
        ctx.chasePlayer()
        if (currentDuration <= 0f) {
            if (abs(xPosition - ctx.phyCmp.body.position.x) < EPS) {
                logger.debug { "CHASE HUNG" }
                return Status.FAILED
            } else {
                xPosition = ctx.phyCmp.body.position.x
                currentDuration = DURATION_TIMER
            }
        } else {
            currentDuration -= GdxAI.getTimepiece().deltaTime
        }
        return Status.RUNNING
    }

    override fun exit() {
        currentDuration = DURATION_TIMER
        ctx.nearestPlatformLedge = null
        ctx.intentionCmp.wantsToChase = false
    }

    companion object {
        val logger = logger<Chase>()
    }
}
