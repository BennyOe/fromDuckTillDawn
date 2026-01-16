package io.bennyoe.ai.actions.spector

import com.badlogic.gdx.ai.btree.Task
import com.badlogic.gdx.ai.btree.annotation.TaskAttribute
import io.bennyoe.ai.blackboards.SpectorContext
import io.bennyoe.ai.core.AbstractAction
import io.bennyoe.components.GameMood
import io.bennyoe.config.GameConstants.WALK_MAX_SPEED
import ktx.log.logger
import ktx.math.vec2

class SpectorMoveTo(
    @JvmField
    @TaskAttribute(required = true)
    var target: String = "",
    @JvmField
    @TaskAttribute(required = true)
    var speed: Float = WALK_MAX_SPEED,
) : AbstractAction<SpectorContext>() {
    var targetPos = vec2()
    private var isFirstFrame = true

    override fun enter() {
        isFirstFrame = true
        ctx.lastTaskName = this.javaClass.simpleName
        targetPos = ctx.getPositionToGoTo(target).cpy()
        ctx.moveCmp.maxWalkSpeed = speed
        if (targetPos.x < ctx.phyCmp.body.position.x) ctx.imageCmp.flipImage = true else ctx.imageCmp.flipImage = false
        ctx.currentMood = GameMood.NORMAL
    }

    override fun onExecute(): Status {
        if (ctx.moveToPosition(targetPos)) {
            return Status.SUCCEEDED
        }

        if (!isFirstFrame && ctx.abortMoveDueToWallOrGap()) {
            ctx.stopMovement()
            return Status.SUCCEEDED
        }

        isFirstFrame = false
        return Status.RUNNING
    }

    override fun exit() {
        ctx.moveCmp.maxWalkSpeed = ctx.initialWalkSpeed
    }

    // the copyTo must be overridden when @TaskAttribute is specified
    override fun copyTo(task: Task<SpectorContext>): Task<SpectorContext> {
        (task as SpectorMoveTo).target = target
        task.speed = speed
        return task
    }

    companion object {
        val logger = logger<SpectorMoveTo>()
    }
}
