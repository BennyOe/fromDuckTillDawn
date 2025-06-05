package io.bennyoe.utility

import com.github.quillraven.fleks.Entity
import io.bennyoe.config.EntityCategory

data class BodyData(
    val type: EntityCategory,
    val entity: Entity,
)

data class FixtureData(
    val type: FixtureType,
)

enum class FixtureType {
    HITBOX_SENSOR,
    COLLISION_SENSOR,
    GROUND_SENSOR,
    NEARBY_ENEMY_SENSOR,
}
