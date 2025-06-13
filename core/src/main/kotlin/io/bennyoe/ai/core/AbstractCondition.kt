package io.bennyoe.ai.core

import com.badlogic.gdx.ai.btree.LeafTask
import com.badlogic.gdx.ai.btree.Task
import io.bennyoe.ai.blackboards.MushroomContext

abstract class AbstractCondition : LeafTask<MushroomContext>() {
    val entity: MushroomContext
        get() = `object`

    abstract fun condition(): Boolean

    override fun execute(): Status =
        when {
            condition() -> Status.SUCCEEDED
            else -> Status.FAILED
        }

    override fun copyTo(task: Task<MushroomContext?>?): Task<MushroomContext?>? = task
}
