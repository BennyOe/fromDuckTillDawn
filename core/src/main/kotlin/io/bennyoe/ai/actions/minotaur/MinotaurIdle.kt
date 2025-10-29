package io.bennyoe.ai.actions.minotaur

import com.badlogic.gdx.ai.GdxAI
import com.badlogic.gdx.ai.btree.Task
import com.badlogic.gdx.ai.btree.annotation.TaskAttribute
import com.badlogic.gdx.ai.utils.random.FloatDistribution
import io.bennyoe.ai.blackboards.MinotaurContext
import io.bennyoe.ai.core.AbstractAction
import io.bennyoe.components.GameMood
import ktx.log.logger

class MinotaurIdle(
    @JvmField
    @TaskAttribute(required = true)
    var duration: FloatDistribution? = null,
) : AbstractAction<MinotaurContext>() {
    private var currentDuration = 0f

    override fun enter() {
        MinotaurAttack.Companion.logger.debug { "Idle Enter" }
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

    override fun exit() {
    }

    // the copyTo must be overridden when @TaskAttribute is specified
    override fun copyTo(task: Task<MinotaurContext>): Task<MinotaurContext> {
        (task as MinotaurIdle).duration = duration
        return task
    }

    companion object {
        val logger = logger<MinotaurIdle>()
    }
}
