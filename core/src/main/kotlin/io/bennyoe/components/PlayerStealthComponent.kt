package io.bennyoe.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

class PlayerStealthComponent : Component<PlayerStealthComponent> {
    var illumination: Float = 0f

    override fun type() = PlayerStealthComponent

    companion object : ComponentType<PlayerStealthComponent>()
}
