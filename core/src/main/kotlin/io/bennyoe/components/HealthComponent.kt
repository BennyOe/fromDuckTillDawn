package io.bennyoe.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

class HealthComponent(
    var current: Float = 30f,
    var max: Float = 30f,
    var takenDamage: Float = 0f,
) : Component<HealthComponent> {
    val isDead get() = current <= 0

    fun resetHealth() {
        current = max
    }

    override fun type() = HealthComponent

    companion object : ComponentType<HealthComponent>()
}
