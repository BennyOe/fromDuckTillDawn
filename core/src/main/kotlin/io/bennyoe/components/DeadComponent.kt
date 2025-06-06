package io.bennyoe.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

data class DeadComponent(
    var keepCorpse: Boolean = false,
    var removeDelay: Float = 0f,
) : Component<DeadComponent> {
    override fun type() = DeadComponent

    companion object : ComponentType<DeadComponent>()
}
