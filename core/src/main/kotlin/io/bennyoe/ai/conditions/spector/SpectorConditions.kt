package io.bennyoe.ai.conditions.spector

import io.bennyoe.ai.blackboards.SpectorContext
import io.bennyoe.ai.core.AbstractCondition

class CanAttack : AbstractCondition<SpectorContext>() {
    override fun condition(): Boolean = entity.canAttack()
}

class HasPlayerNearby : AbstractCondition<SpectorContext>() {
    override fun condition(): Boolean = entity.hasPlayerNearby()
}

class PlayerInChaseRange : AbstractCondition<SpectorContext>() {
    override fun condition(): Boolean = entity.isPlayerInChaseRange()
}

class IsAlive : AbstractCondition<SpectorContext>() {
    override fun condition(): Boolean = entity.isAlive()
}

/**
 * Determines whether the mushroom should enter or remain in the chase state.
 *
 * The chase starts only if the player is within a short detection radius (`hasPlayerNearby()`),
 * but once chasing has started, it continues as long as the player remains within the larger
 * chase range (`playerIsInChaseRange()`).
 *
 * This allows for a hysteresis-like behavior where the mushroom does not immediately stop
 * chasing if the player briefly exits the short-range detection zone.
 */
class ShouldChase : AbstractCondition<SpectorContext>() {
    override fun condition(): Boolean =
        (entity.lastTaskName == "SpectorChase" && entity.isPlayerInChaseRange()) ||
            entity.hasPlayerNearby()
}
