package io.bennyoe.systems.debug

import io.bennyoe.config.GameConstants.ENABLE_DEBUG

object DebugPropsManager {
    @Volatile
    var renderer: DebugRenderer? = null

    private val suppliers = mutableMapOf<String, () -> Any>()

    fun bind(renderer: DebugRenderer) {
        this.renderer = renderer
    }

    fun register(
        name: String,
        supplier: () -> Any,
    ) {
        suppliers[name] = supplier
    }

    fun flush() {
        if (!ENABLE_DEBUG) return
        val r = renderer ?: return

        for ((name, supplier) in suppliers) {
            r.addProperty(name, supplier())
        }
    }
}

/*
To register a property for the debug view, make sure to take the correct entity and register it like this

if (entity has PlayerComponent) {
    DebugPropsManager.register("groundContact") { physicCmp.activeGroundContacts }
}
 */
