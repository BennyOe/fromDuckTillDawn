package io.bennyoe.ai

import com.badlogic.gdx.ai.btree.LeafTask
import com.badlogic.gdx.ai.btree.Task

abstract class Condition : LeafTask<AiContext>() {
    val entity: AiContext
        get() = `object`

    abstract fun condition(): Boolean

    override fun execute(): Status =
        when {
            condition() -> Status.SUCCEEDED
            else -> Status.FAILED
        }

    override fun copyTo(task: Task<AiContext?>?): Task<AiContext?>? = task
}

class CanAttack : Condition() {
    override fun condition(): Boolean = entity.canAttack()
}

class IsEnemyNearby : Condition() {
    override fun condition(): Boolean = entity.hasEnemyNearby()
}

class IsAlive : Condition() {
    override fun condition(): Boolean = entity.isAlive()
}
