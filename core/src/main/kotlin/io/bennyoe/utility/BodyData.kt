package io.bennyoe.utility

import com.github.quillraven.fleks.Entity
import io.bennyoe.config.EntityCategory

data class BodyData(
    val type: EntityCategory,
    val entity: Entity,
)

data class FixtureData(
    val type: SensorType,
)

enum class SensorType {
    HITBOX_SENSOR,
    GROUND_SENSOR,
    NEARBY_ENEMY_SENSOR,
    WALL_SENSOR,
    WALL_HEIGHT_SENSOR,
    JUMP_SENSOR,
    UPPER_LEDGE_SENSOR,
    LOWER_LEDGE_SENSOR,
    ATTACK_SENSOR,
    SIGHT_SENSOR,
}
