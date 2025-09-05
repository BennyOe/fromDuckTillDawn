package io.bennyoe.components

import com.badlogic.gdx.graphics.Color
import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

class HitEffectComponent(
    val color: Color = Color.GREEN,
    var duration: Float = 0.55f,
    var timer: Float = 0f,
) : Component<HitEffectComponent> {
    val isFinished: Boolean
        get() = timer >= duration

    override fun type() = HitEffectComponent

    companion object : ComponentType<HitEffectComponent>()
}
