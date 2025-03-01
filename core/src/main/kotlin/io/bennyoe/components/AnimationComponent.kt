package io.bennyoe.components

import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

class AnimationComponent(
    var stateTime: Float = 0f,
    var playMode: Animation.PlayMode = Animation.PlayMode.LOOP
) : Component<AnimationComponent> {

    override fun type() = AnimationComponent
    lateinit var animation: Animation<TextureRegionDrawable>
    var nextAnimation: String = NO_ANIMATION
        private set

    fun nextAnimation(type: AnimationType) {
        nextAnimation = type.atlasKey
    }

    fun clearAnimation() {
        nextAnimation = NO_ANIMATION
    }

    companion object : ComponentType<AnimationComponent>() {
        const val NO_ANIMATION = ""
    }
}

enum class AnimationType(
    val atlasKey: String
) {
    IDLE("idle01");
}

