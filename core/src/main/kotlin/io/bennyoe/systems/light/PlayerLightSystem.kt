package io.bennyoe.systems.light

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World
import io.bennyoe.components.ImageComponent
import io.bennyoe.components.LightComponent
import io.bennyoe.components.PlayerComponent
import io.bennyoe.lightEngine.core.GameLight

class PlayerLightSystem : IteratingSystem(World.family { all(LightComponent, ImageComponent, PlayerComponent) }) {
    override fun onTickEntity(entity: Entity) {
        val lightCmp = entity[LightComponent]
        val imageCmp = entity[ImageComponent]

        if (lightCmp.gameLight is GameLight.Spot) {
            val spotLight = lightCmp.gameLight
            spotLight.direction = if (imageCmp.flipImage) 180f else 0f
        }
    }
}
