package io.bennyoe.ai

import com.badlogic.gdx.ai.GdxAI
import com.badlogic.gdx.ai.btree.LeafTask
import com.badlogic.gdx.ai.btree.Task
import com.badlogic.gdx.ai.btree.annotation.TaskAttribute
import com.badlogic.gdx.ai.utils.random.FloatDistribution
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Polyline
import com.badlogic.gdx.math.Vector2
import io.bennyoe.ai.blackboards.MushroomContext
import io.bennyoe.components.WalkDirection
import io.bennyoe.service.addToDebugView
import io.bennyoe.systems.debug.DebugType
import ktx.log.logger

abstract class Action : LeafTask<MushroomContext>() {
    val ctx: MushroomContext
        get() = `object`

    private var entered = false

    final override fun execute(): Status {
        if (!entered) {
            entered = true
            enter()
        }
        return onExecute()
    }

    override fun end() {
        if (entered) {
            exit()
            entered = false
        }
    }

    override fun toString(): String = javaClass.simpleName.dropLast(4).uppercase()

    override fun copyTo(task: Task<MushroomContext>) = task

    protected abstract fun enter()

    protected abstract fun onExecute(): Status

    protected open fun exit() {}

    protected open fun drawWalkingLine(
        startPos: Vector2,
        targetPos: Vector2,
    ) {
        Polyline(floatArrayOf(startPos.x, startPos.y, targetPos.x, targetPos.y)).addToDebugView(
            ctx.debugRenderService,
            Color.RED,
            "walk",
            debugType = DebugType.ENEMY,
        )
    }
}

class IdleTask(
    @JvmField
    @TaskAttribute(required = true)
    var duration: FloatDistribution? = null,
) : Action() {
    private var currentDuration = 0f

    override fun enter() {
        ctx.stopAttack()
        ctx.lastTaskName = this.javaClass.simpleName
        ctx.stopMovement()
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
    override fun copyTo(task: Task<MushroomContext>): Task<MushroomContext> {
        (task as IdleTask).duration = duration
        return task
    }

    companion object {
        val logger =
            logger<IdleTask>()
    }
}

class PatrolTask : Action() {
    override fun enter() {
        ctx.stopAttack()
        ctx.lastTaskName = this.javaClass.simpleName
        ctx.intentionCmp.walkDirection = WalkDirection.RIGHT
    }

    override fun onExecute(): Status {
        ctx.moveTo()
        return Status.RUNNING
    }

    companion object {
        val logger =
            logger<PatrolTask>()
    }
}

class AttackTask : Action() {
    override fun enter() {
        ctx.lastTaskName = this.javaClass.simpleName
        ctx.startAttack()
    }

    override fun onExecute(): Status {
        if (ctx.isAnimationFinished()) {
            ctx.stopMovement()
            return Status.SUCCEEDED
        }
        return Status.RUNNING
    }
}
