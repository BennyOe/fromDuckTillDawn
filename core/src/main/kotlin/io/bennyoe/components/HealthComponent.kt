package io.bennyoe.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

class HealthComponent(
    var current: Int = 1,
    var max: Int = 1,
) : Component<HealthComponent> {
    val isDead get() = current <= 0

    override fun type() = HealthComponent

    companion object : ComponentType<HealthComponent>()
}
