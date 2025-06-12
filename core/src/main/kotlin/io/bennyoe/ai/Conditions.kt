package io.bennyoe.ai

import com.badlogic.gdx.ai.btree.LeafTask
import com.badlogic.gdx.ai.btree.Task
import io.bennyoe.ai.blackboards.MushroomContext

abstract class Condition : LeafTask<MushroomContext>() {
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

class CanAttack : Condition() {
    override fun condition(): Boolean = entity.canAttack()
}

class CanNotAttack : Condition() {
    override fun condition(): Boolean = !entity.canAttack()
}

class IsEnemyNearby : Condition() {
    override fun condition(): Boolean = entity.hasEnemyNearby()
}

class IsAlive : Condition() {
    override fun condition(): Boolean = entity.isAlive()
}
