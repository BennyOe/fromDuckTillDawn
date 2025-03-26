package io.bennyoe.components

import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.Animation.PlayMode
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

class AnimationComponent(
    var stateTime: Float = 0f,
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

enum class AnimationModel(
    val atlasKey: String
) {
    PLAYER("player")
}

enum class AnimationType(
    val atlasKey: String,
    val playMode: PlayMode = PlayMode.LOOP,
    val speed: Float = 1/8f
) {
    NONE(""),
    IDLE("player/idle01"),
    WALK("player/walking01"),
    JUMP(
        atlasKey = "player/jump01",
        playMode = PlayMode.NORMAL
    ),
    ATTACK(
        atlasKey = "player/attack04",
        PlayMode.NORMAL,
        speed = 1 / 14f
    );
}
