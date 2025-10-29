package io.bennyoe.ai.core

import com.badlogic.gdx.ai.btree.LeafTask
import com.badlogic.gdx.ai.btree.Task
import io.bennyoe.ai.blackboards.AbstractBlackboard
import io.bennyoe.ai.blackboards.MinotaurContext

abstract class AbstractCondition<T : AbstractBlackboard> : LeafTask<T>() {
    val entity: T
        get() = `object`

    abstract fun condition(): Boolean

    override fun execute(): Status =
        when {
            condition() -> Status.SUCCEEDED
            else -> Status.FAILED
        }

    override fun copyTo(task: Task<T?>?): Task<T?>? = task
}
