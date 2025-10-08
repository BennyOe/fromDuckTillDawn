package io.bennyoe.components

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.physics.box2d.Body
import com.badlogic.gdx.physics.box2d.joints.RopeJoint
import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

data class ChainRenderComponent(
    var joint: RopeJoint,
    var bodyA: Body,
    var bodyB: Body,
    var texture: TextureRegion,
    val segmentHeight: Float,
) : Component<ChainRenderComponent> {
    override fun type() = ChainRenderComponent

    companion object : ComponentType<ChainRenderComponent>()
}
