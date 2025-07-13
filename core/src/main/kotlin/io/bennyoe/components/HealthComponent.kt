package io.bennyoe.components

import com.badlogic.gdx.math.MathUtils
import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

class HealthComponent(
    var current: Float = 30f,
    var max: Float = 30f,
    var takenDamage: Float = 0f,
    var attackedFromBehind: Boolean = false,
) : Component<HealthComponent> {
    val isDead get() = current <= 0

    fun resetHealth() {
        current = max
    }

    fun takeDamage(damage: Float) {
        takenDamage += damage * MathUtils.random(0.9f, 1.1f)
    }

    override fun type() = HealthComponent

    companion object : ComponentType<HealthComponent>()
}
