package io.bennyoe.ai

import io.bennyoe.ai.core.AbstractCondition

class CanAttack : AbstractCondition() {
    override fun condition(): Boolean = entity.canAttack()
}

class HasPlayerNearby : AbstractCondition() {
    override fun condition(): Boolean = entity.hasPlayerNearby()
}

class PlayerInChaseRange : AbstractCondition() {
    override fun condition(): Boolean = entity.playerIsInChaseRange()
}

class IsAlive : AbstractCondition() {
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
class ShouldChase : AbstractCondition() {
    override fun condition(): Boolean =
        (entity.lastTaskName == "Chase" && entity.playerIsInChaseRange()) ||
            entity.hasPlayerNearby()
}
