package io.bennyoe.components.ai

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Vector2
import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import io.bennyoe.components.TransformComponent
import io.bennyoe.config.EntityCategory
import io.bennyoe.utility.EntityBodyData
import io.bennyoe.utility.SensorType
import ktx.collections.gdxArrayOf
import ktx.math.vec2

class BasicSensorsComponent(
    val chaseRange: Float,
    val transformCmp: TransformComponent,
    val maxSightRadius: Float,
) : Component<BasicSensorsComponent> {
    var from = Vector2()
    var to = Vector2()
    val upperLedgeSensorArray = gdxArrayOf<SensorDef>(ordered = true)
    val lowerLedgeSensorArray = gdxArrayOf<SensorDef>(ordered = true)

    val wallSensor =
        SensorDef(
            bodyAnchorPoint = vec2(1f, -0.8f),
            rayLengthOffset = vec2(0.5f, 0f),
            type = SensorType.WALL_SENSOR,
            isHorizontal = true,
            name = "wall sensor",
            color = Color.BLUE,
            hitFilter = { it.entityCategory == EntityCategory.GROUND || it.entityCategory == EntityCategory.WORLD_BOUNDARY },
        )

    // WallHeightSensor is checked if the entity can jump over the obstacle
    val wallHeightSensor =
        SensorDef(
            bodyAnchorPoint = vec2(1f, 1.5f),
            rayLengthOffset = vec2(0.5f, 0f),
            type = SensorType.WALL_HEIGHT_SENSOR,
            isHorizontal = true,
            name = "wall height sensor",
            color = Color.BLUE,
            hitFilter = { it.entityCategory == EntityCategory.GROUND },
        )
    val groundSensor =
        SensorDef(
            bodyAnchorPoint = vec2(1f, -1f),
            rayLengthOffset = vec2(0f, -1.4f),
            type = SensorType.GROUND_DETECT_SENSOR,
            isHorizontal = false,
            name = "ground sensor",
            color = Color.GREEN,
        )
    val jumpSensor =
        SensorDef(
            bodyAnchorPoint = vec2(3.2f, -1f),
            rayLengthOffset = vec2(0f, -1.4f),
            type = SensorType.JUMP_SENSOR,
            false,
            "jump sensor",
            Color.GREEN,
        )
    val sightSensor =
        SensorDef(
            bodyAnchorPoint = vec2(0f, 0f),
            rayLengthOffset = vec2(0f, 0f),
            type = SensorType.SIGHT_SENSOR,
            isHorizontal = false,
            name = "sight sensor",
            color = Color.WHITE,
            hitFilter = { it.entityCategory == EntityCategory.GROUND },
        )
    val attackSensor =
        SensorDef(
            bodyAnchorPoint = vec2(0f, -0.7f),
            rayLengthOffset = vec2(4f, 0f),
            type = SensorType.ATTACK_SENSOR,
            isHorizontal = true,
            name = "attack sensor",
            color = Color.ORANGE,
            hitFilter = { it.entityCategory == EntityCategory.PLAYER },
        )

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
