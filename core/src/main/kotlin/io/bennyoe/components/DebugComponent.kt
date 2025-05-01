package io.bennyoe.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import ktx.log.logger

class DebugComponent(
    var enabled: Boolean = false,
) : Component<DebugComponent> {
    private var alreadyChanged: Boolean = false

    fun toggleDebug(pressed: Boolean) {
        if (pressed && !alreadyChanged) {
            enabled = !enabled
            alreadyChanged = true
            logger.debug { "DEBUG IS ${if (enabled) "ENABLED" else "DISABLED"}" }
        }
        if (!pressed) {
            alreadyChanged = false
        }
    }

    override fun type() = DebugComponent

    companion object : ComponentType<DebugComponent>() {
        val logger = logger<DebugComponent>()
    }
}
