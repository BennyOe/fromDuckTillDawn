package io.bennyoe.components

import com.github.bennyOe.gdxNormalLight.core.GameLight
import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

class LightComponent(
    val gameLight: GameLight,
) : Component<LightComponent> {
    override fun type() = LightComponent

    companion object : ComponentType<LightComponent>()
}
