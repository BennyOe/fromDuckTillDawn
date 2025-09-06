package io.bennyoe.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import io.bennyoe.config.GameConstants.CAMERA_ZOOM_FACTOR

class CameraComponent(
    var zoomFactor: Float = CAMERA_ZOOM_FACTOR,
) : Component<CameraComponent> {
    override fun type() = CameraComponent

    companion object : ComponentType<CameraComponent>()
}
