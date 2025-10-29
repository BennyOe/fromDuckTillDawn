package io.bennyoe.ai.core

import com.badlogic.gdx.ai.btree.LeafTask
import com.badlogic.gdx.ai.btree.Task
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Polyline
import com.badlogic.gdx.math.Vector2
import io.bennyoe.ai.blackboards.AbstractBlackboard
import io.bennyoe.ai.blackboards.MinotaurContext
import io.bennyoe.systems.debug.DebugType
import io.bennyoe.systems.debug.addToDebugView

abstract class AbstractAction<T : AbstractBlackboard> : LeafTask<T>() {
    val ctx: T
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

    override fun copyTo(task: Task<T>) = task

    protected abstract fun enter()

    protected abstract fun onExecute(): Status

    protected open fun exit() {}

    protected open fun drawWalkingLine(
        startPos: Vector2,
        targetPos: Vector2,
    ) {
        Polyline(floatArrayOf(startPos.x, startPos.y, targetPos.x, targetPos.y)).addToDebugView(
            ctx.debugRenderer,
            Color.RED,
            "walk",
            debugType = DebugType.ENEMY,
        )
    }
}
