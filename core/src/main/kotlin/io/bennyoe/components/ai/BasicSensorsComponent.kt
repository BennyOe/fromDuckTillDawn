package io.bennyoe.components.ai

import com.badlogic.gdx.math.Vector2
import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import io.bennyoe.utility.SensorType
import ktx.collections.gdxArrayOf
import ktx.math.plus
import ktx.math.vec2

class BasicSensorsComponent(
    val chaseRange: Float,
) : Component<BasicSensorsComponent> {
    val upperLedgeSensorArray = gdxArrayOf<RayDef>(ordered = true)
    val lowerLedgeSensorArray = gdxArrayOf<RayDef>(ordered = true)

    val wallSensor = RayDef(vec2(0f, -0.6f), vec2(1.5f, 0f), SensorType.WALL_SENSOR, true, false, 0f)
    val wallHeightSensor = RayDef(vec2(0f, 0.5f), vec2(1.5f, 0f), SensorType.WALL_HEIGHT_SENSOR, true, false, 0f)
    val groundSensor = RayDef(vec2(0.5f, 0f), vec2(0f, -1.6f), SensorType.GROUND_SENSOR, false, false, 0f)
    val jumpSensor = RayDef(vec2(2.2f, 0f), vec2(0f, -1.6f), SensorType.JUMP_SENSOR, false, false, 0f)

    init {
        createUpperLedgeSensors()
    }

    private fun createUpperLedgeSensors() {
        for (i in -10..10) {
            upperLedgeSensorArray.add(
                RayDef(
                    vec2(
                        i / 2f,
                        0f,
                    ),
                    vec2(0f, 2f),
                    SensorType.UPPER_LEDGE_SENSOR,
                    false,
                    false,
                    0f,
                ),
            )
            lowerLedgeSensorArray.add(
                RayDef(
                    vec2(
                        i / 2f,
                        -2f,
                    ),
                    vec2(0f, 2f),
                    SensorType.LOWER_LEDGE_SENSOR,
                    false,
                    false,
                    0f,
                ),
            )
        }
    }

    override fun type() = BasicSensorsComponent

    companion object : ComponentType<BasicSensorsComponent>()
}

data class RayDef(
    var fromRelative: Vector2,
    val toRelative: Vector2,
    val tag: SensorType,
    val isHorizontal: Boolean,
    val hit: Boolean,
    val xCoordinate: Float,
) {
    var from = Vector2()
    var to = Vector2()

    // updates the relative ray position with the body position
    fun updateAbsolute(
        bodyPos: Vector2,
        flipImage: Boolean,
    ) {
        if (isHorizontal) {
            from.set(bodyPos).add(fromRelative)
            to =
                if (flipImage) {
                    vec2(from.x - toRelative.x, from.y + toRelative.y)
                } else {
                    vec2(from.x + toRelative.x, from.y + toRelative.y)
                }
        } else {
            val locationOffsetX = if (flipImage) -fromRelative.x else fromRelative.x
            from.set(bodyPos + vec2(locationOffsetX, fromRelative.y))
            to.set(from.x + toRelative.x, from.y + toRelative.y)
        }
    }
}
