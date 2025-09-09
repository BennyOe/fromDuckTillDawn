package io.bennyoe.systems.entitySpawn

import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.maps.MapLayer
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.github.quillraven.fleks.World
import io.bennyoe.components.ImageComponent
import io.bennyoe.components.TransformComponent
import io.bennyoe.components.WaterComponent
import io.bennyoe.config.GameConstants.UNIT_SCALE
import io.bennyoe.utility.setupShader
import ktx.log.logger
import ktx.math.vec2
import ktx.tiled.height
import ktx.tiled.width
import ktx.tiled.x
import ktx.tiled.y

class WaterSpawner(
    private val world: World,
    private val stage: Stage,
    private val phyWorld: com.badlogic.gdx.physics.box2d.World,
    private val worldObjectsAtlas: TextureAtlas,
) {
    fun spawnWater(mapObject: MapLayer) {
        val zIndex = mapObject.properties.get("zIndex", Int::class.java) ?: 0
        mapObject.objects.forEach { waterObject ->

            logger.debug {
                "Water spawned at position: ${waterObject.x} ${waterObject.y} width: ${waterObject.width} height: ${waterObject.height}"
            }
            world.entity {
                val imageCmp = ImageComponent(stage, zIndex = zIndex)
                imageCmp.image = Image()
                it += imageCmp
                it +=
                    TransformComponent(
                        vec2(waterObject.x * UNIT_SCALE, waterObject.y * UNIT_SCALE),
                        waterObject.width * UNIT_SCALE,
                        waterObject.height * UNIT_SCALE,
                    )

                val waterCmp = WaterComponent()
                waterCmp.shader = setupShader("water")
                waterCmp.uniforms.putAll(
                    mapOf(
                        "u_speed" to 0.1f,
                        "u_speed_x" to 0.15f,
                        "u_speed_y" to 0.15f,
                        "u_emboss" to 0.2f,
                        "u_intensity" to 1.5f,
                        "u_frequency" to 4.0f,
                        "u_delta" to 500.0f,
                        "u_distortion_scale" to 0.02f,
                    ),
                )
                it += waterCmp
            }
        }
    }

    companion object {
        val logger = logger<WaterSpawner>()
    }
}
