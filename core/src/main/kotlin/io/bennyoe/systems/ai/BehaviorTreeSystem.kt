package io.bennyoe.systems.ai

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import io.bennyoe.ai.blackboards.HasAwareness
import io.bennyoe.components.ai.BehaviorTreeComponent
import io.bennyoe.systems.PausableSystem

class BehaviorTreeSystem :
    IteratingSystem(family { all(BehaviorTreeComponent) }),
    PausableSystem {
    override fun onTickEntity(entity: Entity) {
        val aiCmp = entity[BehaviorTreeComponent]

        // tick awareness only for blackboards that support it
        (aiCmp.blackboard as? HasAwareness<*>)?.updateAwareness()
        aiCmp.behaviorTree.step()
    }
}
