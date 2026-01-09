package io.bennyoe.ai.actions.spector

import com.badlogic.gdx.ai.GdxAI
import com.badlogic.gdx.ai.btree.annotation.TaskAttribute
import com.badlogic.gdx.ai.utils.random.FloatDistribution
import com.badlogic.gdx.ai.utils.random.IntegerDistribution
import io.bennyoe.ai.blackboards.SpectorContext
import io.bennyoe.ai.core.AbstractAction
import io.bennyoe.components.GameMood
import io.bennyoe.components.WalkDirection
import ktx.log.logger

class SpectorPatrol(
    @JvmField
    @TaskAttribute(required = true)
    var duration: FloatDistribution? = null,
    @JvmField
    @TaskAttribute(required = true)
    var direction: IntegerDistribution? = null,
) : AbstractAction<SpectorContext>() {
    private var currentDuration = 0f

    override fun enter() {
        logger.debug { "Patrol Enter" }
        ctx.lastTaskName = this.javaClass.simpleName
        ctx.currentMood = GameMood.NORMAL
        ctx.intentionCmp.walkDirection = WalkDirection.entries[direction?.nextInt() ?: 1]
        currentDuration = duration?.nextFloat() ?: 1f
    }

    override fun onExecute(): Status {
        ctx.patrol()
        currentDuration -= GdxAI.getTimepiece().deltaTime
        if (currentDuration <= 0f) {
            return Status.SUCCEEDED
        }
        return Status.RUNNING
    }

    companion object {
        val logger = logger<SpectorPatrol>()
    }
}
