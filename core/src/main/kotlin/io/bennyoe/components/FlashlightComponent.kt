package io.bennyoe.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import io.bennyoe.lightEngine.core.GameLight

class FlashlightComponent(
    val spotlight: GameLight.Spot,
    val pointLight: GameLight.Point,
) : Component<FlashlightComponent> {
    var flashlightIsOn: Boolean = false

    override fun type() = FlashlightComponent

    fun toggleFlashlight() {
        flashlightIsOn = !flashlightIsOn
        spotlight.toggle()
        pointLight.toggle()
    }

    companion object : ComponentType<FlashlightComponent>()
}
