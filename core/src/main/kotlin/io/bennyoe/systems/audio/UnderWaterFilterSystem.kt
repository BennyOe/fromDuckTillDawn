package io.bennyoe.systems.audio

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import io.bennyoe.components.PhysicComponent
import io.bennyoe.components.PlayerComponent

class UnderWaterFilterSystem : IteratingSystem(family { all(PlayerComponent, PhysicComponent) }) {
    private val reverb = world.system<ReverbSystem>()

    override fun onTickEntity(entity: Entity) {
        val physicCmp = entity[PhysicComponent]
        if (!physicCmp.isUnderWater) {
            reverb.setGlobalFilters(1f, 1f)
        } else {
            reverb.setGlobalFilters(1f, 0.002f)
        }
    }
}
