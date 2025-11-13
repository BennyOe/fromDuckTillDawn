package io.bennyoe.systems.render

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import io.bennyoe.components.PhysicComponent
import io.bennyoe.components.TransformComponent

// This system synchronizes the body position, width and height to the TransformComponent each frame after updating the physics
class PhysicTransformSyncSystem : IteratingSystem(family { all(PhysicComponent, TransformComponent) }) {
    override fun onTickEntity(entity: Entity) {
        val physicCmp = entity[PhysicComponent]

        val transformCmp = entity[TransformComponent]
        transformCmp.position.set(physicCmp.body.position)
    }
}
