package io.bennyoe.systems

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import io.bennyoe.components.HealthComponent
import ktx.log.logger

class DamageSystem : IteratingSystem(family { all(HealthComponent) }) {
    override fun onTickEntity(entity: Entity) {
        val healthCmp = entity[HealthComponent]
        if (healthCmp.takenDamage > 0f) {
            logger.debug { "takenDamage: ${healthCmp.takenDamage}" }
            healthCmp.current -= healthCmp.takenDamage
            healthCmp.takenDamage = 0f
        }
    }

    companion object {
        val logger = logger<DamageSystem>()
    }
}
