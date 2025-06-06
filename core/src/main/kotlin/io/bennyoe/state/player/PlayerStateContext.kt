package io.bennyoe.state.player

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import io.bennyoe.components.AttackComponent
import io.bennyoe.components.IntentionComponent
import io.bennyoe.state.AbstractStateContext

class PlayerStateContext(
    entity: Entity,
    world: World,
    deltaTime: Float = 0f,
) : AbstractStateContext<PlayerStateContext>(entity, world, deltaTime) {
    val intentionCmp: IntentionComponent by lazy { with(world) { entity[IntentionComponent] } }
    val attackComponent: AttackComponent by lazy { with(world) { entity[AttackComponent] } }

    override val wantsToJump get() = intentionCmp.wantsToJump
    override val wantsToAttack get() = intentionCmp.wantsToAttack
    val wantsToAttack2 get() = intentionCmp.wantsToAttack2
    val wantsToAttack3 get() = intentionCmp.wantsToAttack3
    val wantsToCrouch get() = intentionCmp.wantsToCrouch
    val wantsToBash get() = intentionCmp.wantsToBash
}
