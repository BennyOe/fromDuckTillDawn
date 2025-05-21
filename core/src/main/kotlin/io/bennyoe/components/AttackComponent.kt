package io.bennyoe.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

data class AttackComponent(
    val damage: Float = 5f,
    val maxDamage: Float = 5f,
    val extraRange: Float = 3f,
    var applyAttack: Boolean = false,
) : Component<AttackComponent> {
    override fun type() = AttackComponent

    companion object : ComponentType<AttackComponent>()
}
