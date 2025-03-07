package io.bennyoe.components

import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

class AnimationComponent(
    var stateTime: Float = 0f,
    var playMode: Animation.PlayMode = Animation.PlayMode.LOOP,
    var flipImage: Boolean = false
) : Component<AnimationComponent> {

    override fun type() = AnimationComponent
    lateinit var animation: Animation<TextureRegionDrawable>
    var nextAnimation: AnimationType = AnimationType.NONE
        private set

    fun nextAnimation(type: AnimationType) {
        nextAnimation = type
    }

    fun clearAnimation() {
        nextAnimation = AnimationType.NONE
    }

    companion object : ComponentType<AnimationComponent>() {
    }
}

enum class AnimationType(
    val atlasKey: String
) {
    NONE(""), IDLE("idle01"), WALK("walking01"), JUMP("jump01"), ATTACK("attack04");
}
