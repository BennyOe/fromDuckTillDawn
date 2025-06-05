package io.bennyoe.state.player

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import io.bennyoe.components.AttackComponent
import io.bennyoe.components.InputComponent
import io.bennyoe.components.WalkDirection
import io.bennyoe.state.AbstractStateContext

class PlayerStateContext(
    entity: Entity,
    world: World,
    deltaTime: Float = 0f,
) : AbstractStateContext<PlayerStateContext>(entity, world, deltaTime) {
    val inputComponent: InputComponent by lazy { with(world) { entity[InputComponent] } }
    val attackComponent: AttackComponent by lazy { with(world) { entity[AttackComponent] } }

    override val wantsToJump get() = inputComponent.jumpJustPressed
    override val wantsToAttack get() = inputComponent.attackJustPressed
    val wantsToCrouch get() = inputComponent.crouch
    val wantsToBash get() = inputComponent.bashJustPressed
    val wantsToAttack2 get() = inputComponent.attack2JustPressed
    val wantsToAttack3 get() = inputComponent.attack3JustPressed
}
