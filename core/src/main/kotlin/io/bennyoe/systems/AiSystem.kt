package io.bennyoe.systems

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import io.bennyoe.components.AiComponent

class AiSystem : IteratingSystem(family { all(AiComponent) }) {
    override fun onTickEntity(entity: Entity) {
        val aiCmp = entity[AiComponent]
        aiCmp.behaviorTree.step()
    }
}
