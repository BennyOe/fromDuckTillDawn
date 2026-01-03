package io.bennyoe.components.animation

import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.Animation.PlayMode
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import io.bennyoe.systems.audio.SoundType

class AnimationComponent(
    var stateTime: Float = 0f,
    var mode: PlayMode = PlayMode.LOOP,
    var isReversed: Boolean = false,
    var animationModel: AnimationModel = AnimationModel.PLAYER_DAWN,
    var currentAnimationType: AnimationKey = NoAnimationKey,
    var animationSoundTriggers: Map<AnimationKey, Map<Int, SoundType>> = emptyMap(),
    var speedMultiplier: Float = 1f,
) : Component<AnimationComponent> {
    override fun type() = AnimationComponent

    lateinit var animation: Animation<TextureRegionDrawable>
    var nextAnimationModel: AnimationModel = AnimationModel.NONE
        private set
    var nextAnimationType: AnimationKey = NoAnimationKey
        private set

    var previousFrameIndex: Int = -1

    fun nextAnimation(type: AnimationKey) {
        nextAnimationModel = animationModel
        nextAnimationType = type
    }

    fun clearAnimation() {
        nextAnimationModel = AnimationModel.NONE
        nextAnimationType = NoAnimationKey
    }

    fun isAnimationFinished(): Boolean = animation.isAnimationFinished(stateTime)

    companion object : ComponentType<AnimationComponent>() {
    }
}

enum class AnimationModel(
    val atlasKey: String,
) {
    NONE(""),
    PLAYER_DAWN("player/dawn/"),
    ENEMY_MUSHROOM("enemy/mushroom/"),
    ENEMY_MINOTAUR("enemy/minotaur/"),
    ENEMY_SPECTOR("enemy/spector/"),
    CROW("crow"),
}
