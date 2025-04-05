package io.bennyoe.systems

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import io.bennyoe.components.AnimationComponent
import io.bennyoe.components.AnimationModel
import io.bennyoe.components.AnimationType
import io.bennyoe.components.AnimationVariant
import io.bennyoe.components.AttackComponent
import io.bennyoe.components.PhysicComponent
import io.bennyoe.components.PlayerComponent

private const val BASH_POWER = 35f

class AttackSystem : IteratingSystem(family { all(PlayerComponent) }) {
    override fun onTickEntity(entity: Entity) {
        val attackCmp = entity[AttackComponent]
        val phyCmp = entity[PhysicComponent]
        val animCmp = entity[AnimationComponent]

        if (attackCmp.bashActive) {
            handleBashCooldown(attackCmp)
        }

        if (attackCmp.bashRequest) {
            performBash(attackCmp, animCmp, phyCmp)
        }

        if (attackCmp.attack) {
            performAttack(attackCmp, animCmp)
        }
    }

    private fun performAttack(attackCmp: AttackComponent, animCmp: AnimationComponent){
        attackCmp.attack = false
        animCmp.nextAnimation(AnimationModel.PLAYER_DAWN, AnimationType.ATTACK, AnimationVariant.FIRST)
    }

    private fun handleBashCooldown(attackCmp: AttackComponent) {
        if (attackCmp.bashCooldown <= 0) {
            attackCmp.bashActive = false
            attackCmp.resetBash()
        } else {
            attackCmp.bashCooldown -= deltaTime
        }
    }

    private fun performBash(
        attackCmp: AttackComponent,
        animCmp: AnimationComponent,
        phyCmp: PhysicComponent
    ) {
        animCmp.nextAnimation(AnimationModel.PLAYER_DAWN, AnimationType.BASH, AnimationVariant.FIRST)
        attackCmp.bashActive = true

        val bashDirection = if (animCmp.flipImage) -1f else 1f
        val bashStrength = phyCmp.body.mass * BASH_POWER
        phyCmp.impulse.set(bashStrength * bashDirection, 0f)

        attackCmp.bashRequest = false
    }

    companion object {
        val logger = ktx.log.logger<AttackSystem>()
    }
}
