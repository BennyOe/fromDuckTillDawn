package io.bennyoe.ai.conditions.minotaur

import io.bennyoe.ai.blackboards.MinotaurContext
import io.bennyoe.ai.blackboards.MushroomContext
import io.bennyoe.ai.core.AbstractCondition

class CanAttack : AbstractCondition<MinotaurContext>() {
    override fun condition(): Boolean = entity.canAttack()
}

class HasPlayerNearby : AbstractCondition<MinotaurContext>() {
    override fun condition(): Boolean = entity.hasPlayerNearby()
}

class PlayerInChaseRange : AbstractCondition<MinotaurContext>() {
    override fun condition(): Boolean = entity.isPlayerInChaseRange()
}

class IsAlive : AbstractCondition<MinotaurContext>() {
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
class ShouldChase : AbstractCondition<MinotaurContext>() {
    override fun condition(): Boolean =
        (entity.lastTaskName == "Chase" && entity.isPlayerInChaseRange()) ||
            entity.hasPlayerNearby()
}
