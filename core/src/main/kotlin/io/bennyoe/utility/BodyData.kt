package io.bennyoe.utility

import com.github.quillraven.fleks.Entity
import io.bennyoe.config.EntityCategory

data class BodyData(
    val type: EntityCategory,
    val entity: Entity,
    val floorType: FloorType? = null,
)

data class FixtureData(
    val type: SensorType,
)

enum class SensorType {
    NONE,
    HITBOX_SENSOR,
    GROUND_DETECT_SENSOR,
    GROUND_TYPE_SENSOR,
    NEARBY_ENEMY_SENSOR,
    WALL_SENSOR,
    WALL_HEIGHT_SENSOR,
    JUMP_SENSOR,
    UPPER_LEDGE_SENSOR,
    LOWER_LEDGE_SENSOR,
    ATTACK_SENSOR,
    SIGHT_SENSOR,
    AUDIO_EFFECT_SENSOR,
    SOUND_TRIGGER_SENSOR,
}

enum class FloorType {
    WOOD,
    STONE,
    GRASS,
}
