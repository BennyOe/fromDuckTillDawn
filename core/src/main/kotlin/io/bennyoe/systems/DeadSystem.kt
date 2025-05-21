package io.bennyoe.systems

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import io.bennyoe.components.HealthComponent
import io.bennyoe.components.PlayerComponent
import ktx.log.logger

class DeadSystem : IteratingSystem(family { all(HealthComponent) }) {
    override fun onTickEntity(entity: Entity) {
        val healthCmp = entity[HealthComponent]

        if (entity hasNo PlayerComponent && healthCmp.isDead) {
            logger.debug { "Entity ${entity.id} dead, removing." }
            entity.configure { it.remove() }
        }
    }

    companion object {
        val logger = logger<DeadSystem>()
    }
}
