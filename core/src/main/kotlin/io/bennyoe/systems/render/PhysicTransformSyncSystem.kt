package io.bennyoe.systems.render

import com.badlogic.gdx.physics.box2d.BodyDef
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import io.bennyoe.components.PhysicComponent
import io.bennyoe.components.TransformComponent
import ktx.math.vec2

// This system synchronizes the body position, width and height to the TransformComponent for Dynamic-Bodies each frame after updating the physics
class PhysicTransformSyncSystem : IteratingSystem(family { all(PhysicComponent, TransformComponent) }) {
    override fun onTickEntity(entity: Entity) {
        val physicCmp = entity[PhysicComponent]
        if (physicCmp.body.type == BodyDef.BodyType.StaticBody) return

        val transformCmp = entity[TransformComponent]
        transformCmp.position.set(vec2(physicCmp.body.position.x, physicCmp.body.position.y))
        transformCmp.width = physicCmp.size.x
        transformCmp.height = physicCmp.size.y
    }
}
