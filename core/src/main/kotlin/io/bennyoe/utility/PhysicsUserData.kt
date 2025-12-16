package io.bennyoe.utility

import com.github.quillraven.fleks.Entity
import io.bennyoe.config.EntityCategory

sealed class PhysicsUserData

/**
 * Represents the data for a main body, linking it to an entity.
 * @param entity The entity this body belongs to.
 * @param entityCategory The collision category of this entity.
 * @param floorType Optional type of the floor, if this entity is a ground surface.
 */
data class EntityBodyData(
    val entity: Entity,
    val entityCategory: EntityCategory,
    val floorType: FloorType? = null,
) : PhysicsUserData()

/**
 * Represents data for a specific fixture, detailing its purpose.
 * @param entity The entity this fixture belongs to, for easy access.
 * @param sensorType The specific type of sensor or fixture.
 */
data class FixtureSensorData(
    val entity: Entity,
    val sensorType: SensorType,
) : PhysicsUserData()

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
    SOUND_AMBIENCE_SENSOR,
    IN_WATER_SENSOR,
    UNDER_WATER_SENSOR,
    DOOR_TRIGGER_SENSOR,
    PLAYER_IN_THROW_RANGE_SENSOR,
}

enum class FloorType {
    WOOD,
    STONE,
    GRASS,
}
