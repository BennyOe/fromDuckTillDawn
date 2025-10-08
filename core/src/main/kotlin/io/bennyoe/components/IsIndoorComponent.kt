package io.bennyoe.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

class IsIndoorComponent(
    val isIndoor: Boolean = false,
) : Component<IsIndoorComponent> {
    override fun type() = IsIndoorComponent

    companion object : ComponentType<IsIndoorComponent>()
}
