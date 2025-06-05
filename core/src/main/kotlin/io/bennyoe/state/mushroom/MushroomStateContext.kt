package io.bennyoe.state.mushroom

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import io.bennyoe.components.AttackComponent
import io.bennyoe.components.InputComponent
import io.bennyoe.state.AbstractStateContext

class MushroomStateContext(
    entity: Entity,
    world: World,
    deltaTime: Float = 0f,
) : AbstractStateContext<MushroomStateContext>(entity, world, deltaTime) {
    val inputComponent: InputComponent by lazy { with(world) { entity[InputComponent] } }
    val attackComponent: AttackComponent by lazy { with(world) { entity[AttackComponent] } }

    override val wantsToJump get() = false
    override val wantsToAttack get() = inputComponent.attackJustPressed
}
