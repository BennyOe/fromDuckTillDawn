package io.bennyoe.systems.entitySpawn

import com.badlogic.gdx.maps.MapLayer
import com.badlogic.gdx.physics.box2d.BodyDef
import com.github.quillraven.fleks.World
import io.bennyoe.components.PhysicComponent
import io.bennyoe.components.TransformComponent
import io.bennyoe.components.WaterComponent
import io.bennyoe.config.EntityCategory
import io.bennyoe.config.GameConstants.UNIT_SCALE
import io.bennyoe.systems.physic.WaterColumn
import io.bennyoe.utility.EntityBodyData
import io.bennyoe.utility.FixtureSensorData
import io.bennyoe.utility.SensorType
import io.bennyoe.utility.setupShader
import ktx.box2d.body
import ktx.box2d.box
import ktx.log.logger
import ktx.math.vec2
import ktx.tiled.height
import ktx.tiled.width
import ktx.tiled.x
import ktx.tiled.y
import kotlin.experimental.or
import kotlin.math.max

const val WATER_DETAIL = 0.02f

class WaterSpawner(
    private val world: World,
    private val phyWorld: com.badlogic.gdx.physics.box2d.World,
) {
    fun spawnWater(mapObject: MapLayer) {
        mapObject.objects.forEach { waterObject ->

            logger.debug {
                "Water spawned at position: ${waterObject.x} ${waterObject.y} width: ${waterObject.width} height: ${waterObject.height}"
            }
            world.entity { entity ->
                val position = vec2(waterObject.x * UNIT_SCALE, waterObject.y * UNIT_SCALE)
                val width = waterObject.width * UNIT_SCALE
                val height = waterObject.height * UNIT_SCALE
                val centerX = position.x + width * 0.5f
                val centerY = position.y + height * 0.5f
                val transformCmp =
                    TransformComponent(
                        position.cpy(),
                        width,
                        height,
                    )
                entity += transformCmp

                val waterCmp = WaterComponent()
                waterCmp.shader = setupShader("water")
                waterCmp.uniforms.putAll(
                    mapOf(
                        // Slow horizontal movement of the water texture
                        "u_flowSpeed" to 0.006f,
                        // Speed of large waves
                        "u_waveSpeed" to 0.2f,
                        // How many large waves are on screen
                        "u_waveFrequency" to 8.0f,
                        // Amplitude/height of large waves
                        "u_waveHeight" to 0.005f,
                        // Speed of small ripples
                        "u_rippleSpeed" to 0.8f,
                        // How many small ripples are on screen
                        "u_rippleFrequency" to 25.0f,
                        // Amplitude/height of small ripples
                        "u_rippleHeight" to 0.003f,
                        // chaos tuning
                        "u_chaosAmount" to 0.28f,
                        "u_chaosScale" to 3.5f,
                        "u_flowJitter" to 0.6f,
                    ),
                )
                entity += waterCmp

                val physicCmp = PhysicComponent()
                physicCmp.body =
                    phyWorld.body(BodyDef.BodyType.StaticBody) {
                        position.set(position.x + width * 0.5f, position.y + height * 0.5f)
                        fixedRotation = true
                        allowSleep = false
                        userData = EntityBodyData(entity, EntityCategory.WATER)

                        // Use a box shape for the sensor to detect the entire area, not just the vertices
                        box(
                            width,
                            height,
                            position = vec2(centerX, centerY),
                        ) {
                            this.isSensor = true
                            this.userData = FixtureSensorData(entity, SensorType.IN_WATER_SENSOR)
                            filter.categoryBits = EntityCategory.WATER.bit
                            filter.maskBits =
                                EntityCategory.PLAYER.bit or
                                EntityCategory.ENEMY.bit or
                                EntityCategory.SENSOR.bit
                            density = waterCmp.density
                            friction = 0.2f
                        }.disposeOfShape
                    }
                entity += physicCmp
                initializeWaveColumns(centerX, width, centerY, height, waterCmp)
            }
        }
    }

    private fun initializeWaveColumns(
        centerX: Float,
        width: Float,
        centerY: Float,
        height: Float,
        waterCmp: WaterComponent,
    ) {
        val startX = centerX - width / 2f
        val bottomY = centerY - height / 2f
        val topY = centerY + height / 2f

        // Number of segments across the width (ensure at least 1)
        val segments = max(1, (width / WATER_DETAIL).toInt())

        repeat(segments + 1) { i ->
            val cx = startX + i * WATER_DETAIL
            waterCmp.columns.add(WaterColumn(cx, bottomY, topY, topY, 0f))
        }
    }

    companion object {
        val logger = logger<WaterSpawner>()
    }
}
