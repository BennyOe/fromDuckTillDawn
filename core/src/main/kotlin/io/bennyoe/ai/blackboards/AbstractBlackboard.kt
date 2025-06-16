package io.bennyoe.ai.blackboards

import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World

abstract class AbstractBlackboard(
    protected val entity: Entity,
    protected val world: World,
    protected val stage: Stage,
) {
    var lastTaskName: String? = null
}
