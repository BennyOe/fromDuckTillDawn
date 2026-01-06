package io.bennyoe.ai.actions.spector

import com.badlogic.gdx.ai.GdxAI
import com.badlogic.gdx.ai.btree.Task
import com.badlogic.gdx.ai.btree.annotation.TaskAttribute
import com.badlogic.gdx.ai.utils.random.FloatDistribution
import com.badlogic.gdx.math.Vector2
import io.bennyoe.ai.blackboards.SpectorContext
import io.bennyoe.ai.core.AbstractAction
import io.bennyoe.components.GameMood
import ktx.log.logger

class SpectorMoveTo(
    @JvmField
    @TaskAttribute
    var target: String? = null
) : AbstractAction<SpectorContext>() {

    override fun enter() {
        SpectorAttack.logger.debug { "CancelChase Enter" }
        ctx.lastTaskName = this.javaClass.simpleName
        ctx.currentMood = GameMood.NORMAL
    }

    override fun onExecute(): Status {

            return Status.SUCCEEDED
        return Status.RUNNING
    }

    override fun exit() {
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
