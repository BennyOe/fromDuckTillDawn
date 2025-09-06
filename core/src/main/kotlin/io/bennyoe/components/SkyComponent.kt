package io.bennyoe.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

class SkyComponent(
    val type: SkyComponentType,
) : Component<SkyComponent> {
    override fun type() = SkyComponent

    companion object : ComponentType<SkyComponent>()
}

enum class SkyComponentType {
    SKY,
    STARS,
    SUN,
    MOON,
    SHOOTING_STAR,
}
