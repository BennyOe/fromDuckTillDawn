package io.bennyoe.components.ai

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Vector2
import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import io.bennyoe.config.EntityCategory
import io.bennyoe.utility.BodyData
import io.bennyoe.utility.SensorType
import ktx.collections.gdxArrayOf
import ktx.math.plus
import ktx.math.vec2

class BasicSensorsComponent(
    val chaseRange: Float,
) : Component<BasicSensorsComponent> {
    val upperLedgeSensorArray = gdxArrayOf<SensorDef>(ordered = true)
    val lowerLedgeSensorArray = gdxArrayOf<SensorDef>(ordered = true)

    val wallSensor =
        SensorDef(
            fromRelative = vec2(0f, -0.6f),
            toRelative = vec2(1f, 0f),
            type = SensorType.WALL_SENSOR,
            isHorizontal = true,
            name = "wall sensor",
            color = Color.BLUE,
            hitFilter = { it.type == EntityCategory.GROUND },
        )
    val wallHeightSensor =
        SensorDef(
            fromRelative = vec2(0f, 0.5f),
            toRelative = vec2(1f, 0f),
            type = SensorType.WALL_HEIGHT_SENSOR,
            isHorizontal = true,
            name = "wall height sensor",
            color = Color.BLUE,
            hitFilter = { it.type == EntityCategory.GROUND },
        )
    val groundSensor = SensorDef(vec2(0.5f, 0f), vec2(0f, -1.6f), SensorType.GROUND_SENSOR, false, "ground sensor", Color.GREEN)
    val jumpSensor = SensorDef(vec2(2.2f, 0f), vec2(0f, -1.6f), SensorType.JUMP_SENSOR, false, "jump sensor", Color.GREEN)
    val sightSensor =
        SensorDef(
            fromRelative = vec2(0f, 0f),
            toRelative = vec2(0f, 0f),
            type = SensorType.SIGHT_SENSOR,
            isHorizontal = false,
            name = "sight sensor",
            color = Color.WHITE,
            hitFilter = { it.type == EntityCategory.GROUND },
        )
    val attackSensor =
        SensorDef(
            fromRelative = vec2(0f, -0.6f),
            toRelative = vec2(1.5f, 0f),
            type = SensorType.ATTACK_SENSOR,
            isHorizontal = true,
            name = "attack sensor",
            color = Color.ORANGE,
            // filter on this type of hit entity
            hitFilter = { it.type == EntityCategory.PLAYER },
        )

    init {
        createUpperLedgeSensors()
    }

    private fun createUpperLedgeSensors() {
        for (i in -10..10) {
            upperLedgeSensorArray.add(
                SensorDef(
                    fromRelative = vec2(i / 2f, 0f),
                    toRelative = vec2(0f, 2f),
                    type = SensorType.UPPER_LEDGE_SENSOR,
                    isHorizontal = false,
                    name = "upper ledge sensor",
                    hitFilter = { it.type == EntityCategory.GROUND },
                ),
            )
            lowerLedgeSensorArray.add(
                SensorDef(
                    vec2(
                        i / 2f,
                        -2f,
                    ),
                    vec2(0f, 2f),
                    SensorType.LOWER_LEDGE_SENSOR,
                    false,
                    "lower ledge sensor",
                ),
            )
        }
    }

    override fun type() = BasicSensorsComponent

    companion object : ComponentType<BasicSensorsComponent>()
}

data class SensorDef(
    var fromRelative: Vector2,
    val toRelative: Vector2,
    val type: SensorType,
    val isHorizontal: Boolean,
    val name: String,
    val color: Color = Color.BLUE,
    val highlightColor: Color = Color.RED,
    val hitFilter: ((BodyData) -> Boolean)? = null,
) {
    var from = Vector2()
    var to = Vector2()

    // updates the relative ray position with the body position
    fun updateAbsolutePositions(
        bodyPos: Vector2,
        flipImage: Boolean,
    ) {
        if (isHorizontal) {
            if (type == SensorType.ATTACK_SENSOR) {
                from =
                    if (flipImage) {
                        vec2(bodyPos.x + fromRelative.x + 0.5f, bodyPos.y + fromRelative.y)
                    } else {
                        vec2(bodyPos.x + fromRelative.x - 0.5f, bodyPos.y + fromRelative.y)
                    }
            } else {
                from.set(bodyPos).add(fromRelative)
            }

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

    fun updateSightSensor(
        bodyPos: Vector2,
        playerBodyPos: Vector2,
    ) {
        from.set(bodyPos).add(fromRelative)
        to.set(playerBodyPos).add(toRelative)
    }
}
