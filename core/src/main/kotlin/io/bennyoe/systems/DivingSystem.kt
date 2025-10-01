package io.bennyoe.systems

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import io.bennyoe.components.HealthComponent
import io.bennyoe.components.IsDivingComponent

class DivingSystem : IteratingSystem(family { all(IsDivingComponent, HealthComponent) }) {
    override fun onTickEntity(entity: Entity) {
        val isDivingCmp = entity[IsDivingComponent]
        val healthCmp = entity[HealthComponent]

        if (isDivingCmp.currentAir > 0f) {
            isDivingCmp.currentAir -= deltaTime
        } else {
            healthCmp.current -= deltaTime * 10
        }
    }
}
