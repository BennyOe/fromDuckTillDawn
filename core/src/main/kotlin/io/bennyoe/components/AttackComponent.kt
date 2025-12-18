package io.bennyoe.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import io.bennyoe.config.CharacterType

data class AttackComponent(
    var attackMap: Map<AttackType, Attack> = mapOf(),
    var appliedAttack: AttackType = AttackType.NONE,
) : Component<AttackComponent> {
    override fun type() = AttackComponent

    companion object : ComponentType<AttackComponent>()
}

data class Attack(
    val attackType: AttackType,
    val attacker: CharacterType,
    val baseDamage: Float = 5f,
    val maxDamage: Float = 5f,
    val extraRange: Float = 1f,
    val attackDelay: Float = 0.2f,
    val hasHitStop: Boolean = false,
    val hitStopDuration: Float = 0f,
)

enum class AttackType {
    NONE,
    SWORD,
    HEADNUT,
    AXE,
    BASH,
    SHAKE,
}
