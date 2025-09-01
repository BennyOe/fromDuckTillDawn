package io.bennyoe.systems.entitySpawn

import com.badlogic.gdx.maps.MapLayer
import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.quillraven.fleks.World
import io.bennyoe.components.RainMaskComponent
import io.bennyoe.config.GameConstants.UNIT_SCALE
import ktx.tiled.height
import ktx.tiled.width
import ktx.tiled.x
import ktx.tiled.y

class RainMaskSpawner(
    val world: World,
    val stage: Stage,
) {
    fun spawnRainMasks(rainMaskLayer: MapLayer) {
        rainMaskLayer.objects.forEach { rainMask ->
            println("Shape: ${rainMask.javaClass}")
            world.entity {
                it +=
                    RainMaskComponent(
                        rainMask.x * UNIT_SCALE,
                        rainMask.y * UNIT_SCALE,
                        rainMask.width * UNIT_SCALE,
                        rainMask.height * UNIT_SCALE,
                    )
            }
        }
    }
}
