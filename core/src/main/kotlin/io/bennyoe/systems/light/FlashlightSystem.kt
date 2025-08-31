package io.bennyoe.systems.light

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World
import io.bennyoe.components.FlashlightComponent
import io.bennyoe.components.ImageComponent
import io.bennyoe.components.PlayerComponent
import io.bennyoe.components.TransformComponent
import ktx.math.vec2
import kotlin.math.truncate

class FlashlightSystem :
    IteratingSystem(
        World.family {
            all(
                FlashlightComponent,
                ImageComponent,
                PlayerComponent,
                TransformComponent,
            )
        },
    ) {
    override fun onTickEntity(entity: Entity) {
        val flashLightCmp = entity[FlashlightComponent]
        val imageCmp = entity[ImageComponent]
        val transformCmp = entity[TransformComponent]

        val offsetX = if (imageCmp.flipImage) -0.5f else 0.5f

        flashLightCmp.spotlight.direction = if (imageCmp.flipImage) 180f else 0f
        flashLightCmp.spotlight.position = transformCmp.position
        flashLightCmp.pointLight.position = vec2(transformCmp.position.x + offsetX, transformCmp.position.y + 0.2f)
    }
}
