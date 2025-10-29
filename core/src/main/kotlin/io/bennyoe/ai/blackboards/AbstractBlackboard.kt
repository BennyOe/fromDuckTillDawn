package io.bennyoe.ai.blackboards

import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import io.bennyoe.components.GameMood
import io.bennyoe.systems.debug.DebugRenderer

const val Y_THRESHOLD = 0.3f
const val X_THRESHOLD = 0.1f

abstract class AbstractBlackboard(
    protected val entity: Entity,
    protected val world: World,
    protected val stage: Stage,
    val debugRenderer: DebugRenderer,
) {
    var lastTaskName: String? = null
    var currentMood: GameMood = GameMood.NORMAL
}

enum class PlatformRelation { SAME, ABOVE, BELOW }
