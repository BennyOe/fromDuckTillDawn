package io.bennyoe.ai

import com.badlogic.gdx.ai.GdxAI
import com.badlogic.gdx.ai.btree.LeafTask
import com.badlogic.gdx.ai.btree.Task
import com.badlogic.gdx.ai.btree.annotation.TaskAttribute
import com.badlogic.gdx.ai.utils.random.FloatDistribution
import com.badlogic.gdx.math.MathUtils
import io.bennyoe.components.AnimationType
import ktx.log.logger
import ktx.math.vec2

abstract class Action : LeafTask<AiContext>() {
    val entity: AiContext
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

    protected abstract fun enter()

    protected abstract fun onExecute(): Status

    protected open fun exit() {}

    override fun copyTo(task: Task<AiContext>) = task
}

class IdleTask(
    @JvmField
    @TaskAttribute(required = true)
    var duration: FloatDistribution? = null,
) : Action() {
    private var currentDuration = 0f

    override fun enter() {
        logger.debug { "Entering Idle state" }
        entity.setAnimation(AnimationType.IDLE)
        currentDuration = duration?.nextFloat() ?: 1f
    }

    override fun onExecute(): Status {
        logger.debug { "In Idle state" }
        // GdxAi.getTimepiece() has to be updated in the render() of the screen
        currentDuration -= GdxAI.getTimepiece().deltaTime
        if (currentDuration <= 0f) {
            return Status.SUCCEEDED
        }
        return Status.RUNNING
    }

    override fun exit() {
        logger.debug { "Exiting Idle state" }
    }

    // the copyTo must be overridden when @TaskAttribute is specified
    override fun copyTo(task: Task<AiContext>): Task<AiContext> {
        (task as IdleTask).duration = duration
        return task
    }

    companion object {
        val logger =
            logger<IdleTask>()
    }
}

class WanderTask : Action() {
    private val startPos = vec2()
    private val targetPos = vec2()

    override fun enter() {
        IdleTask.Companion.logger.debug { "Entering Wander state" }
        entity.setAnimation(AnimationType.WALK)
        if (startPos.isZero) {
            startPos.set(entity.location)
        }
        targetPos.set(startPos)
        targetPos.x += MathUtils.random(-3f, 3f)
        entity.moveTo(startPos, targetPos)
    }

    override fun onExecute(): Status {
//        IdleTask.Companion.logger.debug { "In Wander state" }
        if (entity.inRange(0.5f, targetPos)) {
            logger.debug { "wander succeeded" }
            entity.stopMovement()
            return Status.SUCCEEDED
        }
        return Status.RUNNING
    }

    companion object {
        val logger =
            logger<WanderTask>()
    }
}
