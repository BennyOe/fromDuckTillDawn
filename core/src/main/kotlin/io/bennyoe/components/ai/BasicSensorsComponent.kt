package io.bennyoe.components.ai

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Vector2
import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import io.bennyoe.components.TransformComponent
import io.bennyoe.utility.EntityBodyData
import io.bennyoe.utility.SensorType

class BasicSensorsComponent(
    val sensorList: List<SensorDef>,
    val chaseRange: Float,
    val transformCmp: TransformComponent,
    val maxSightRadius: Float,
    val sightSensorDef: SensorDef? = null,
) : Component<BasicSensorsComponent> {
    var from = Vector2()
    var to = Vector2()

    override fun type() = BasicSensorsComponent

    companion object : ComponentType<BasicSensorsComponent>()
}

data class SensorDef(
    /**
     * Defines the starting point relative to the body center.
     * Values are factors of half-width/half-height.
     * E.g., (1f, 0f) starts at the right edge (center y).
     * (0f, -1f) starts at the bottom edge (center x).
     */
    var bodyAnchorPoint: Vector2,
    /**
     * The vector of the raycast in World Units, relative to the start position.
     * E.g., (1f, 0f) creates a ray of length 1 to the right.
     */
    val rayLengthOffset: Vector2,
    val type: SensorType,
    val isHorizontal: Boolean,
    val name: String,
    val color: Color = Color.BLUE,
    val highlightColor: Color = Color.RED,
    val hitFilter: ((EntityBodyData) -> Boolean)? = null,
) {
    var from = Vector2()
    var to = Vector2()

    fun updateAbsolutePositions(
        fixtureCenterPos: Vector2,
        flipImage: Boolean,
        bodySize: Vector2,
    ) {
        val halfW = bodySize.x * 0.5f
        val halfH = bodySize.y * 0.5f

        // Set direction multiplier for clean mirroring (-1 for left, 1 for right)
        val direction = if (flipImage) -1f else 1f

        // 1. Calculate the 'from' position in world coordinates, applying the direction.
        // This is equivalent to your: val locationOffsetX = if (flipImage) -fromRelative.x else fromRelative.x
        // 1. 'fromRelative' is a MULTIPLIER of the body size (half-width)
        // x = 1f means: Start exactly at the edge of the hitbox (1 * halfW)
        val fromX = fixtureCenterPos.x + (bodyAnchorPoint.x * halfW * direction)
        val fromY = fixtureCenterPos.y + (bodyAnchorPoint.y * halfH)
        from.set(fromX, fromY)

        // 2. Calculate the 'to' position by applying the offset to the FINAL 'from' position.
        // The offset's x-component is also multiplied by the direction.
        // This is equivalent to your: to.set(from.x + toRelative.x, from.y + toRelative.y)
        // but works correctly for ALL sensors because toRelative.x is also mirrored.
        val toX = from.x + (rayLengthOffset.x * direction)
        val toY = from.y + rayLengthOffset.y
        to.set(toX, toY)
    }

    fun updateSightSensor(
        bodyPos: Vector2,
        playerBodyPos: Vector2,
    ) {
        from.set(bodyPos).add(bodyAnchorPoint)
        to.set(playerBodyPos).add(rayLengthOffset)
    }
}
