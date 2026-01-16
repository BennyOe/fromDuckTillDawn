package io.bennyoe.ai.blackboards

import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import io.bennyoe.components.GameMood
import io.bennyoe.systems.debug.DebugRenderer

abstract class AbstractBlackboard(
    protected open val entity: Entity,
    protected val stage: Stage,
    open val world: World,
    val debugRenderer: DebugRenderer,
) {
    var lastTaskName: String? = null
    var currentMood: GameMood = GameMood.NORMAL
}

interface HasAwareness<T> {
    var awareness: T

    fun updateAwareness()
}

enum class PlatformRelation { SAME, PLAYER_BELOW, PLAYER_ABOVE }
