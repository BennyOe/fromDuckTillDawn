package io.bennyoe.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

class CloudComponent : Component<CloudComponent> {
    var speed: Float = 0f
    var parallaxFactor: Float = 1f

    override fun type() = CloudComponent

    companion object : ComponentType<CloudComponent>()
}
