package io.bennyoe.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

class AttackComponent(
    var attack: Boolean = false,
    var bashRequest: Boolean = false,
    var bashActive: Boolean = false,
    var bashCooldown: Float = .3f,
) : Component<AttackComponent> {
    override fun type() = AttackComponent

    fun resetBash(){
        bashCooldown = .3f
    }

    companion object : ComponentType<AttackComponent>()
}
