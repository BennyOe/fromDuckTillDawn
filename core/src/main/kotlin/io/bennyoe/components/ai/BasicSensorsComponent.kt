package io.bennyoe.components.ai

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Vector2
import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import io.bennyoe.config.EntityCategory
import io.bennyoe.utility.EntityBodyData
import io.bennyoe.utility.SensorType
import ktx.collections.gdxArrayOf
import ktx.math.plus
import ktx.math.vec2

class BasicSensorsComponent(
    val chaseRange: Float,
) : Component<BasicSensorsComponent> {
    var from = Vector2()
    var to = Vector2()
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
            hitFilter = { it.entityCategory == EntityCategory.GROUND || it.entityCategory == EntityCategory.WORLD_BOUNDARY },
        )
    val wallHeightSensor =
        SensorDef(
            fromRelative = vec2(0f, 0.5f),
            toRelative = vec2(1f, 0f),
            type = SensorType.WALL_HEIGHT_SENSOR,
            isHorizontal = true,
            name = "wall height sensor",
            color = Color.BLUE,
            hitFilter = { it.entityCategory == EntityCategory.GROUND },
        )
    val groundSensor = SensorDef(vec2(0.5f, 0f), vec2(0f, -1.6f), SensorType.GROUND_DETECT_SENSOR, false, "ground sensor", Color.GREEN)
    val jumpSensor = SensorDef(vec2(2.2f, 0f), vec2(0f, -1.6f), SensorType.JUMP_SENSOR, false, "jump sensor", Color.GREEN)
    val sightSensor =
        SensorDef(
            fromRelative = vec2(0f, 0f),
            toRelative = vec2(0f, 0f),
            type = SensorType.SIGHT_SENSOR,
            isHorizontal = false,
            name = "sight sensor",
            color = Color.WHITE,
            hitFilter = { it.entityCategory == EntityCategory.GROUND },
        )
    val attackSensor =
        SensorDef(
            fromRelative = vec2(-0.2f, -0.6f),
            toRelative = vec2(1.5f, 0f),
            type = SensorType.ATTACK_SENSOR,
            isHorizontal = true,
            name = "attack sensor",
            color = Color.ORANGE,
            // filter on this type of hit entity
            hitFilter = { it.entityCategory == EntityCategory.PLAYER },
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
                    hitFilter = { it.entityCategory == EntityCategory.GROUND },
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
    val hitFilter: ((EntityBodyData) -> Boolean)? = null,
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
                val newFromX = if (flipImage) bodyPos.x + fromRelative.x + 0.5f else bodyPos.x + fromRelative.x - 0.5f
                from.set(newFromX, bodyPos.y + fromRelative.y)
            } else {
                from.set(bodyPos).add(fromRelative)
            }

            val newToX = if (flipImage) from.x - toRelative.x else from.x + toRelative.x
            to.set(newToX, from.y + toRelative.y)
        } else {
            val locationOffsetX = if (flipImage) -fromRelative.x else fromRelative.x
            from.set(bodyPos).add(locationOffsetX, fromRelative.y)
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
