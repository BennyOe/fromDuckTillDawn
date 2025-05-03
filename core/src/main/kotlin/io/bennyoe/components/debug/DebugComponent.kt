package io.bennyoe.components.debug

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import ktx.log.logger

class DebugComponent(
    var enabled: Boolean = false,
) : Component<DebugComponent> {
    private var alreadyChanged: Boolean = false

    override fun World.onAdd(entity: Entity) {
    }

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
