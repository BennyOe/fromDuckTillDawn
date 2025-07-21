package io.bennyoe.ai.actions

import com.badlogic.gdx.ai.GdxAI
import com.badlogic.gdx.ai.btree.annotation.TaskAttribute
import com.badlogic.gdx.ai.utils.random.FloatDistribution
import io.bennyoe.ai.core.AbstractAction
import io.bennyoe.components.GameMood
import io.bennyoe.components.WalkDirection
import ktx.log.logger

class Patrol(
    @JvmField
    @TaskAttribute(required = true)
    var duration: FloatDistribution? = null,
) : AbstractAction() {
    private var currentDuration = 0f

    override fun enter() {
        logger.debug { "Patrol Enter" }
        ctx.stopAttack()
        ctx.lastTaskName = this.javaClass.simpleName
        ctx.currentMood = GameMood.NORMAL
        ctx.intentionCmp.walkDirection = WalkDirection.RIGHT
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
        val logger = logger<Patrol>()
    }
}
