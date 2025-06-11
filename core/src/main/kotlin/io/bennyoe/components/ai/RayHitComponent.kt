package io.bennyoe.components.ai

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

class RayHitComponent : Component<RayHitComponent> {
    var wallHit = false
    var groundHit = false
    var jumpHit = false

    override fun type() = RayHitComponent

    companion object : ComponentType<RayHitComponent>()
}
