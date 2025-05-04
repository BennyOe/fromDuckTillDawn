package io.bennyoe.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import io.bennyoe.GameConstants.BASH_COOLDOWN
import io.bennyoe.GameConstants.BASH_POWER

class BashComponent(
    var bashCooldown: Float = BASH_COOLDOWN,
    val bashPower: Float = BASH_POWER,
) : Component<BashComponent> {
    override fun type() = BashComponent

    companion object : ComponentType<BashComponent>()
}
