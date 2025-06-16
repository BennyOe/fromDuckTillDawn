package io.bennyoe.systems

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import io.bennyoe.components.ai.BehaviorTreeComponent

class BehaviorTreeSystem :
    IteratingSystem(family { all(BehaviorTreeComponent) }),
    PausableSystem {
    override fun onTickEntity(entity: Entity) {
        val aiCmp = entity[BehaviorTreeComponent]
        aiCmp.behaviorTree.step()
    }
}
