package io.bennyoe.components

import com.badlogic.gdx.graphics.Color
import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import io.bennyoe.config.GameConstants.HIT_STOP_DURATION

class HitEffectComponent(
    val color: Color = Color.WHITE,
    var duration: Float = HIT_STOP_DURATION,
    var timer: Float = 0f,
) : Component<HitEffectComponent> {
    val isFinished: Boolean
        get() = timer >= duration

    override fun type() = HitEffectComponent

    companion object : ComponentType<HitEffectComponent>()
}
