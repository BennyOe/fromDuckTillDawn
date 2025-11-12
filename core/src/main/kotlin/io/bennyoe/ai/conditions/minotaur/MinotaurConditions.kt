package io.bennyoe.ai.conditions.minotaur

import io.bennyoe.ai.blackboards.MinotaurContext
import io.bennyoe.ai.core.AbstractCondition
import io.bennyoe.state.minotaur.MinotaurFSM

class CanAttack : AbstractCondition<MinotaurContext>() {
    override fun condition(): Boolean = entity.canAttack()
}

class SeesPlayer : AbstractCondition<MinotaurContext>() {
    override fun condition(): Boolean {
        val isBusy =
            entity.fsmStateIs<MinotaurFSM.HIT>() ||
                entity.fsmStateIs<MinotaurFSM.DEATH>() ||
                entity.fsmStateIs<MinotaurFSM.STUNNED>() ||
                entity.fsmStateIs<MinotaurFSM.SPIN_ATTACK_START>() ||
                entity.fsmStateIs<MinotaurFSM.SPIN_ATTACK_LOOP>() ||
                entity.fsmStateIs<MinotaurFSM.GRABBING>() ||
                entity.fsmStateIs<MinotaurFSM.SHAKING>() ||
                entity.fsmStateIs<MinotaurFSM.THROWING_PLAYER>() ||
                entity.fsmStateIs<MinotaurFSM.THROWING_ROCK>() ||
                entity.fsmStateIs<MinotaurFSM.STOMP>()

        // If busy, we don't "see" the player for decision-making purposes
        if (isBusy) {
            return false
        }

        // Otherwise, base it on the raycast
        return entity.seesPlayer()
    }
}

class PlayerGrabable : AbstractCondition<MinotaurContext>() {
    override fun condition(): Boolean = entity.playerInGrabRange()
}

class PlayerInThrowRange : AbstractCondition<MinotaurContext>() {
    override fun condition(): Boolean = entity.playerInThrowRange()
}

class PlayerInStompRange : AbstractCondition<MinotaurContext>() {
    override fun condition(): Boolean = entity.playerInStompAttackRange() && entity.playerIsInAir()
}

class IsAlive : AbstractCondition<MinotaurContext>() {
    override fun condition(): Boolean = entity.isAlive()
}
