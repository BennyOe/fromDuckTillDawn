package io.bennyoe.systems

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import io.bennyoe.components.AnimationComponent
import io.bennyoe.components.DeadComponent
import io.bennyoe.components.HealthComponent
import io.bennyoe.components.PhysicComponent
import io.bennyoe.components.ai.BehaviorTreeComponent
import ktx.log.logger

class ExpireSystem :
    IteratingSystem(family { all(DeadComponent) }),
    PausableSystem {
    override fun onTickEntity(entity: Entity) {
        val aniCmp = entity[AnimationComponent]
        val deadCmp = entity[DeadComponent]
        val healthComponent = entity[HealthComponent]
        val physicCmp = entity[PhysicComponent]

        if (!healthComponent.isDead) return

        if (deadCmp.removeDelayCounter - deltaTime > 0f) {
            deadCmp.removeDelayCounter -= deltaTime
            return
        }
        physicCmp.body.apply { isActive = false }

        if (aniCmp.isAnimationFinished()) {
            when (deadCmp.keepCorpse) {
                true -> {
                    // Only access BehaviorTreeComponent if the entity has it
                    if (entity.has(BehaviorTreeComponent)) {
                        val behaviorTreeCmp = entity[BehaviorTreeComponent]
                        behaviorTreeCmp.behaviorTree.`object`.lastTaskName = null
                    }
                    logger.debug { "Dead Component removed" }
                }

                false -> {
                    logger.debug { "Entity removed" }
                    with(world) {
                        entity.remove()
                    }
                }
            }
        }
    }

    companion object {
        val logger = logger<ExpireSystem>()
    }
}
