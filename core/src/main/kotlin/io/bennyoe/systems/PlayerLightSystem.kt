package io.bennyoe.systems

import com.github.bennyOe.gdxNormalLight.core.GameLight
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import io.bennyoe.components.ImageComponent
import io.bennyoe.components.LightComponent
import io.bennyoe.components.PlayerComponent

class PlayerLightSystem : IteratingSystem(family { all(LightComponent, ImageComponent, PlayerComponent) }) {
    override fun onTickEntity(entity: Entity) {
        val lightCmp = entity[LightComponent]
        val imageCmp = entity[ImageComponent]

        if (lightCmp.gameLight is GameLight.Spot) {
            val spotLight = lightCmp.gameLight
            spotLight.direction = if (imageCmp.flipImage) 180f else 0f
        }
    }
}
