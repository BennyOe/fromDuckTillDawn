package io.bennyoe.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

class CameraComponent(
    var zoomFactor: Float = 1f,
) : Component<CameraComponent> {
    override fun type() = CameraComponent

    companion object : ComponentType<CameraComponent>()
}
