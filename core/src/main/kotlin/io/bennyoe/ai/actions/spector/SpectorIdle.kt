package io.bennyoe.ai.actions.spector

import com.badlogic.gdx.ai.GdxAI
import com.badlogic.gdx.ai.btree.Task
import com.badlogic.gdx.ai.btree.annotation.TaskAttribute
import com.badlogic.gdx.ai.utils.random.FloatDistribution
import io.bennyoe.ai.blackboards.SpectorContext
import io.bennyoe.ai.core.AbstractAction
import io.bennyoe.components.GameMood
import ktx.log.logger

class SpectorIdle(
    @JvmField
    @TaskAttribute(required = true)
    var duration: FloatDistribution? = null,
) : AbstractAction<SpectorContext>() {
    private var currentDuration = 0f

    override fun enter() {
        ctx.lastTaskName = this.javaClass.simpleName
        ctx.currentMood = GameMood.NORMAL
        ctx.idle()
        currentDuration = duration?.nextFloat() ?: 1f
    }

    override fun onExecute(): Status {
        // GdxAi.getTimepiece() has to be updated in the render() of the screen
        currentDuration -= GdxAI.getTimepiece().deltaTime
        if (currentDuration <= 0f) {
            return Status.SUCCEEDED
        }
        return Status.RUNNING
    }

    // the copyTo must be overridden when @TaskAttribute is specified
    override fun copyTo(task: Task<SpectorContext>): Task<SpectorContext> {
        (task as SpectorIdle).duration = duration
        return task
    }

    companion object {
        val logger = logger<SpectorIdle>()
    }
}
