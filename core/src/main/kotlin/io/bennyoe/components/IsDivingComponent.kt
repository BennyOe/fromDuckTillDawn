package io.bennyoe.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import io.bennyoe.systems.debug.DebugPropsManager

class IsDivingComponent : Component<IsDivingComponent> {
    val maxAir: Float = 8f
    var currentAir: Float = 8f

    init {
        DebugPropsManager.register("currentAir") { currentAir }
    }

    override fun type() = IsDivingComponent

    companion object : ComponentType<IsDivingComponent>()
}
