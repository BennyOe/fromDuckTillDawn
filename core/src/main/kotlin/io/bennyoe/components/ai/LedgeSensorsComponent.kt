package io.bennyoe.components.ai

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import io.bennyoe.config.EntityCategory
import io.bennyoe.utility.SensorType
import ktx.collections.gdxArrayOf
import ktx.math.vec2

class LedgeSensorsComponent : Component<LedgeSensorsComponent> {
    val upperLedgeSensorArray = gdxArrayOf<SensorDef>(ordered = true)
    val lowerLedgeSensorArray = gdxArrayOf<SensorDef>(ordered = true)

    init {
        createUpperLedgeSensors()
    }

    private fun createUpperLedgeSensors() {
        for (i in -10..10) {
            upperLedgeSensorArray.add(
                SensorDef(
                    bodyAnchorPoint = vec2(i / 2f, 0f),
                    rayLengthOffset = vec2(0f, 2f),
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

    override fun type() = LedgeSensorsComponent

    companion object : ComponentType<LedgeSensorsComponent>()
}
