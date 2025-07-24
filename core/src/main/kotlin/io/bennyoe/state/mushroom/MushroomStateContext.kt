package io.bennyoe.state.mushroom

import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import io.bennyoe.components.AttackComponent
import io.bennyoe.components.IntentionComponent
import io.bennyoe.state.AbstractStateContext

class MushroomStateContext(
    entity: Entity,
    world: World,
    stage: Stage,
    deltaTime: Float = 0f,
) : AbstractStateContext<MushroomStateContext>(entity, world, stage, deltaTime) {
    val intentionCmp: IntentionComponent by lazy { with(world) { entity[IntentionComponent] } }
    val attackCmp: AttackComponent by lazy { with(world) { entity[AttackComponent] } }

    override val wantsToJump get() = intentionCmp.wantsToJump
    override val wantsToAttack get() = intentionCmp.wantsToAttack
}
