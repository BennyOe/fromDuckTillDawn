package io.bennyoe.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

class BashComponent(
    var bashCooldown: Float = .3f,
    val bashPower: Float = 5f
) : Component<BashComponent> {
    override fun type() = BashComponent

    companion object : ComponentType<BashComponent>()
}
