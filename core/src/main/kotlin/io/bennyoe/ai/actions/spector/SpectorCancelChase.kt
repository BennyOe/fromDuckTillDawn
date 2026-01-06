package io.bennyoe.ai.actions.spector

import com.badlogic.gdx.ai.GdxAI
import com.badlogic.gdx.ai.btree.Task
import com.badlogic.gdx.ai.btree.annotation.TaskAttribute
import com.badlogic.gdx.ai.utils.random.FloatDistribution
import io.bennyoe.ai.blackboards.SpectorContext
import io.bennyoe.ai.core.AbstractAction
import io.bennyoe.components.GameMood
import ktx.log.logger

class SpectorCancelChase(
    @JvmField
    @TaskAttribute(required = true)
    var duration: FloatDistribution? = null,
) : AbstractAction<SpectorContext>() {
    private var currentDuration = 0f

    override fun enter() {
        SpectorAttack.logger.debug { "CancelChase Enter" }
        ctx.lastTaskName = this.javaClass.simpleName
        ctx.currentMood = GameMood.NORMAL
        currentDuration = duration?.nextFloat() ?: 1f
    }

    override fun onExecute(): Status {
        currentDuration -= GdxAI.getTimepiece().deltaTime
        ctx.cancelChase()

        if (currentDuration <= 0f) {
            ctx.cancelChase()
            return Status.SUCCEEDED
        }
        return Status.RUNNING
    }

    override fun exit() {
    }

    // the copyTo must be overridden when @TaskAttribute is specified
    override fun copyTo(task: Task<SpectorContext>): Task<SpectorContext> {
        (task as SpectorCancelChase).duration = duration
        return task
    }

    companion object {
        val logger = logger<SpectorCancelChase>()
    }
}
