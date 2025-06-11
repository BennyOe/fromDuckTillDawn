package io.bennyoe.components.ai

import com.badlogic.gdx.math.Vector2
import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import ktx.collections.gdxArrayOf

class BasicSensorsComponent(
    val wallSensor: RayDef,
    val groundSensor: RayDef,
    val jumpSensor: RayDef,
) : Component<BasicSensorsComponent> {
    val rays = gdxArrayOf<RayDef>()

    override fun type() = BasicSensorsComponent

    companion object : ComponentType<BasicSensorsComponent>()
}

data class RayDef(
    var locationOffset: Vector2,
    val length: Vector2,
    val tag: RayTag,
)

enum class RayTag {
    GROUND_SENSOR,
    WALL_SENSOR,
    JUMP_SENSOR,
    ATTACK_SENSOR,
    TURN_AROUND_SENSOR,
}
