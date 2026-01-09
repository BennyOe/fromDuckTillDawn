package io.bennyoe.ai.actions.spector

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ai.btree.Task
import com.badlogic.gdx.ai.btree.annotation.TaskAttribute
import com.badlogic.gdx.ai.utils.random.FloatDistribution
import io.bennyoe.ai.blackboards.SpectorContext
import io.bennyoe.ai.core.AbstractAction
import io.bennyoe.components.GameMood
import ktx.log.logger
import ktx.math.vec2

class SpectorMoveTo(
    @JvmField
    @TaskAttribute(required = true)
    var target: String = "",
    @JvmField
    @TaskAttribute
    var speed: FloatDistribution? = null,
) : AbstractAction<SpectorContext>() {
    var targetPos = vec2()

    override fun enter() {
        ctx.lastTaskName = this.javaClass.simpleName
        ctx.currentMood = GameMood.NORMAL
        targetPos = ctx.getPositionToGoTo(target).cpy()
        ctx.investigationIsFinished = false
    }

    override fun onExecute(): Status = if (!ctx.moveToPosition(targetPos)) Status.RUNNING else Status.SUCCEEDED

    override fun exit() {
        ctx.investigationIsFinished = true
    }

    // the copyTo must be overridden when @TaskAttribute is specified
    override fun copyTo(task: Task<SpectorContext>): Task<SpectorContext> {
        (task as SpectorMoveTo).target = target
        return task
    }

    companion object {
        val logger = logger<SpectorMoveTo>()
    }
}
