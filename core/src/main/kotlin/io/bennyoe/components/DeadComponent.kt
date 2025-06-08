package io.bennyoe.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

data class DeadComponent(
    var keepCorpse: Boolean = false,
    var removeDelay: Float = 0f,
    var removeDelayCounter: Float = 0f,
    var isDead: Boolean = false,
) : Component<DeadComponent> {
    override fun type() = DeadComponent

    fun resetRemoveDealyCounter() {
        removeDelayCounter = removeDelay
    }

    companion object : ComponentType<DeadComponent>()
}
