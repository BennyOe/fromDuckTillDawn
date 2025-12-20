package io.bennyoe.systems

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import com.github.quillraven.fleks.World.Companion.inject
import io.bennyoe.components.FlashlightComponent
import io.bennyoe.components.PhysicComponent
import io.bennyoe.components.PlayerStealthComponent
import io.bennyoe.lightEngine.core.Scene2dLightEngine

class PlayerStealthSystem(
    val lightEngine: Scene2dLightEngine = inject("lightEngine"),
) : IteratingSystem(family { all(PlayerStealthComponent, PhysicComponent) }) {
    override fun onTickEntity(entity: Entity) {
        val stealthCmp = entity[PlayerStealthComponent]
        val physicCmp = entity[PhysicComponent]
        val playerFlashlightCmp = entity[FlashlightComponent]
        stealthCmp.illumination =
            if (playerFlashlightCmp.flashlightIsOn) 1f else lightEngine.estimateBrightnessForPlane(physicCmp.body.position, physicCmp.size)
    }
}
