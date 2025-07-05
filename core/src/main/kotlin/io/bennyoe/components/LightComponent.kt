package io.bennyoe.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import io.bennyoe.lightEngine.core.GameLight

class LightComponent(
    val gameLight: GameLight,
) : Component<LightComponent> {
    override fun type() = LightComponent

    companion object : ComponentType<LightComponent>()
}
