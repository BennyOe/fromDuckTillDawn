package io.bennyoe.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import io.bennyoe.lightEngine.core.GameLight

class FlashlightComponent(
    val spotlight: GameLight.Spot,
    val pointLight: GameLight.Point,
) : Component<FlashlightComponent> {
    override fun type() = FlashlightComponent

    companion object : ComponentType<FlashlightComponent>()
}
