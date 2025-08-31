package io.bennyoe.systems.light

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import io.bennyoe.components.LightComponent
import io.bennyoe.components.PhysicComponent
import io.bennyoe.components.TransformComponent
import io.bennyoe.lightEngine.core.GameLight
import ktx.math.vec2

class EntityLightSystem : IteratingSystem(family { all(LightComponent, TransformComponent) }) {
    override fun onTickEntity(entity: Entity) {
        val lightCmp = entity[LightComponent]
        val transformCmp = entity[TransformComponent]

        // TODO check if this is needed when b2dLight.attachToBody(phyCmp.body) is used at spawn
        // with PhysicComponent the center is in the middle of the image
        if (entity has PhysicComponent) {
            when (val light = lightCmp.gameLight) {
                is GameLight.Spot -> light.position = transformCmp.position
                is GameLight.Point -> light.position = transformCmp.position
                else -> Unit
            }
        } else {
            // without PhysicComponent the center is in the bottom left of the image
            val newPos = vec2(transformCmp.position.x + transformCmp.width / 2, transformCmp.position.y + transformCmp.height / 2)
            when (val light = lightCmp.gameLight) {
                is GameLight.Spot -> light.position = newPos
                is GameLight.Point -> light.position = newPos
                else -> Unit
            }
        }
    }
}
