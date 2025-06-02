package io.bennyoe.ai

import com.badlogic.gdx.ai.GdxAI
import com.badlogic.gdx.ai.btree.LeafTask
import com.badlogic.gdx.ai.btree.Task
import com.badlogic.gdx.ai.btree.annotation.TaskAttribute
import com.badlogic.gdx.ai.utils.random.FloatDistribution
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Polyline
import com.badlogic.gdx.math.Vector2
import io.bennyoe.ai.blackboards.MushroomContext
import io.bennyoe.components.AnimationType
import io.bennyoe.service.DebugRenderService
import io.bennyoe.service.addToDebugView
import io.bennyoe.systems.debug.DebugType
import ktx.log.logger
import ktx.math.vec2

abstract class Action : LeafTask<MushroomContext>() {
    val context: MushroomContext
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
            DebugRenderService,
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
        context.lastTaskName = this.javaClass.simpleName
        context.setAnimation(AnimationType.IDLE)
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

class WanderTask : Action() {
    private val startPos = vec2()
    private val targetPos = vec2()

    override fun enter() {
        context.lastTaskName = this.javaClass.simpleName
        context.setAnimation(AnimationType.WALK)
        if (startPos.isZero) {
            startPos.set(context.location)
        }
        targetPos.set(startPos)
        targetPos.x += MathUtils.random(-5f, 5f)
        context.moveTo(targetPos)
    }

    override fun onExecute(): Status {
        drawWalkingLine(startPos, targetPos)
        if (context.inRange(0.5f, targetPos)) {
            context.stopMovement()
            logger.debug { "succeeded" }
            return Status.SUCCEEDED
        }
        return Status.RUNNING
    }

    companion object {
        val logger =
            logger<WanderTask>()
    }
}

class AttackTask : Action() {
    override fun enter() {
        context.lastTaskName = this.javaClass.simpleName
        context.setAnimation(AnimationType.ATTACK, Animation.PlayMode.NORMAL, resetStateTime = true)
        context.startAttack()
    }

    override fun onExecute(): Status {
        if (context.isAnimationFinished()) {
            context.setAnimation(AnimationType.IDLE, Animation.PlayMode.LOOP)
            context.stopMovement()
            return Status.SUCCEEDED
        }
        return Status.RUNNING
    }
}
