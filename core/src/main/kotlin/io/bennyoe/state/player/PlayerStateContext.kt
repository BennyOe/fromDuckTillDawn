package io.bennyoe.state.player

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import io.bennyoe.components.AttackComponent
import io.bennyoe.components.HealthComponent
import io.bennyoe.components.InputComponent
import io.bennyoe.components.JumpComponent
import io.bennyoe.components.MoveComponent
import io.bennyoe.state.AbstractStateContext

class PlayerStateContext(
    entity: Entity,
    world: World,
    deltaTime: Float = 0f,
) : AbstractStateContext(entity, world, deltaTime) {
    val inputComponent: InputComponent
    val moveComponent: MoveComponent
    val jumpComponent: JumpComponent
    val healthComponent: HealthComponent
    val attackComponent: AttackComponent

    init {
        with(world) {
            inputComponent = entity[InputComponent]
            moveComponent = entity[MoveComponent]
            jumpComponent = entity[JumpComponent]
            healthComponent = entity[HealthComponent]
            attackComponent = entity[AttackComponent]
        }
    }
}
