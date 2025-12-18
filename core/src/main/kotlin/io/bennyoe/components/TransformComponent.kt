package io.bennyoe.components

import com.badlogic.gdx.math.Vector2
import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

/**
 * The [TransformComponent] is the single source of truth for the visual representation of
 * [position], [width] and [height]
 * It gets updated each frame by the [io.bennyoe.systems.render.PhysicTransformSyncSystem] and updates the visuals via the
 * [io.bennyoe.systems.render.TransformVisualSyncSystem]
 */
class TransformComponent(
    var position: Vector2,
    var width: Float,
    var height: Float,
) : Component<TransformComponent> {
    override fun type() = TransformComponent

    companion object : ComponentType<TransformComponent>()
}
