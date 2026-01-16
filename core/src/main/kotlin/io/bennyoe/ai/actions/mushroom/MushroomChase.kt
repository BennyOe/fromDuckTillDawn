package io.bennyoe.ai.actions.mushroom

import com.badlogic.gdx.ai.GdxAI
import io.bennyoe.ai.blackboards.MushroomContext
import io.bennyoe.ai.core.AbstractAction
import io.bennyoe.components.GameMood
import io.bennyoe.components.WalkDirection
import ktx.log.logger
import kotlin.math.abs

const val DURATION_TIMER = .5f
const val EPS = 1.0f

class MushroomChase : AbstractAction<MushroomContext>() {
    private var currentDuration = DURATION_TIMER
    private var xPosition = 0f

    override fun enter() {
        logger.debug { "Chase Enter" }
        ctx.lastTaskName = this.javaClass.simpleName
        ctx.currentMood = GameMood.CHASE
        ctx.intentionCmp.wantsToChase = true
        ctx.stopAttack()
        ctx.intentionCmp.walkDirection = WalkDirection.NONE
    }

    override fun onExecute(): Status {
        ctx.chasePlayer(ctx.world)
        return Status.RUNNING
    }

    override fun exit() {
        ctx.nearestPlatformLedge = null
        ctx.intentionCmp.wantsToChase = false
    }

    companion object {
        val logger = logger<MushroomChase>()
    }
}
