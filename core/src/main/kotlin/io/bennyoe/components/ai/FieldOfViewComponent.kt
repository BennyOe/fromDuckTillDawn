package io.bennyoe.components.ai

import com.badlogic.gdx.math.MathUtils
import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import io.bennyoe.components.TransformComponent

class FieldOfViewComponent(
    val transformCmp: TransformComponent,
    var maxDistance: Float,
    val relativeEyePos: Float,
    val numberOfRays: Int = 3,
    var maxVerticalDistance: Float = 12f,
    viewAngle: Float = 90f,
) : Component<FieldOfViewComponent> {
    var fovThreshold: Float = MathUtils.cosDeg(viewAngle * 0.5f)

    init {
        // using squared distance
        maxDistance *= maxDistance
        maxVerticalDistance *= maxVerticalDistance
        require(numberOfRays >= 3) { "The numberOfRays for the field of view has to be >= 3" }
    }

    override fun type() = FieldOfViewComponent

    companion object : ComponentType<FieldOfViewComponent>()
}
