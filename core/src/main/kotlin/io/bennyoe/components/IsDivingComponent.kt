package io.bennyoe.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

class IsDivingComponent : Component<IsDivingComponent> {
    val maxAir: Float = 8f
    var currentAir: Float = 8f

    override fun type() = IsDivingComponent

    companion object : ComponentType<IsDivingComponent>()
}
