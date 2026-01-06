package io.bennyoe.ai.conditions.spector

import io.bennyoe.ai.blackboards.SpectorContext
import io.bennyoe.ai.core.AbstractCondition

class CanAttack : AbstractCondition<SpectorContext>() {
    override fun condition(): Boolean = entity.canAttack()
}

class IsAlive : AbstractCondition<SpectorContext>() {
    override fun condition(): Boolean = entity.isAlive()
}

class Cool : AbstractCondition<SpectorContext>() {
    override fun condition(): Boolean = entity.isCool()
}

class Irritated : AbstractCondition<SpectorContext>() {
    override fun condition(): Boolean = entity.isIrritated()
}

class Suspicious : AbstractCondition<SpectorContext>() {
    override fun condition(): Boolean = entity.isSuspicious()
}

class Identification : AbstractCondition<SpectorContext>() {
    override fun condition(): Boolean = entity.hasIdentified()
}

class CancelChase : AbstractCondition<SpectorContext>() {
    override fun condition(): Boolean = entity.isCancelingChase()
}

class CancelSearching : AbstractCondition<SpectorContext>() {
    override fun condition(): Boolean = entity.isCancelingSearching()
}

class CancelInvestigating : AbstractCondition<SpectorContext>() {
    override fun condition(): Boolean = entity.isCancelingInvestigating()
}
