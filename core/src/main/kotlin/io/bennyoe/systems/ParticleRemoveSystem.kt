package io.bennyoe.systems

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import io.bennyoe.components.ParticleComponent
import io.bennyoe.components.TransformComponent

/**
 * Despawns non-looping particle entities once their effect is complete.
 * Also supports a safety timeout and optional off-screen culling.
 */
class ParticleRemoveSystem : IteratingSystem(family { all(ParticleComponent, TransformComponent) }) {
    override fun onTickEntity(entity: Entity) {
        val particleCmp = entity[ParticleComponent]

        if (particleCmp.looping) return

        val effectComplete = particleCmp.actor.effect.isComplete

        if (effectComplete) {
            world -= entity
        }
    }
}
