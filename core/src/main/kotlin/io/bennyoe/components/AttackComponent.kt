package io.bennyoe.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

data class AttackComponent(
    var baseDamage: Float = 5f,
    var maxDamage: Float = 5f,
    var extraRange: Float = 1f,
    var attackDelay: Float = 0.2f,
    var applyAttack: Boolean = false,
) : Component<AttackComponent> {
    override fun type() = AttackComponent

    companion object : ComponentType<AttackComponent>()
}
