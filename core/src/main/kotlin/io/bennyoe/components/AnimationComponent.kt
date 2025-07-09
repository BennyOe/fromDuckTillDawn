package io.bennyoe.components

import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.Animation.PlayMode
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

class AnimationComponent(
    var stateTime: Float = 0f,
    var mode: PlayMode = PlayMode.LOOP,
    var isReversed: Boolean = false,
    var animationModel: AnimationModel = AnimationModel.PLAYER_DAWN,
    var currentAnimationType: AnimationType = AnimationType.NONE,
) : Component<AnimationComponent> {
    override fun type() = AnimationComponent

    lateinit var animation: Animation<TextureRegionDrawable>
    var nextAnimationModel: AnimationModel = AnimationModel.NONE
        private set
    var nextAnimationType: AnimationType = AnimationType.NONE
        private set
    var nextAnimationVariant: AnimationVariant = AnimationVariant.NONE
        private set

    fun nextAnimation(
        type: AnimationType,
        variant: AnimationVariant = AnimationVariant.FIRST,
    ) {
        nextAnimationModel = animationModel
        nextAnimationType = type
        nextAnimationVariant = variant
    }

    fun clearAnimation() {
        nextAnimationModel = AnimationModel.NONE
        nextAnimationType = AnimationType.NONE
        nextAnimationVariant = AnimationVariant.NONE
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
}

enum class AnimationType(
    val atlasKey: String,
    val playMode: PlayMode = PlayMode.LOOP,
    val speed: Float = 1 / 8f,
    val normalMap: Boolean = true,
    val specularMap: Boolean = true,
) {
    NONE(""),
    IDLE("idle"),
    WALK(
        "walking",
    ),
    JUMP(
        atlasKey = "jump",
        playMode = PlayMode.LOOP,
    ),
    ATTACK(
        atlasKey = "attack",
        PlayMode.NORMAL,
        speed = 1 / 14f,
    ),
    BASH(
        atlasKey = "bash",
        PlayMode.NORMAL,
        speed = 1 / 20f,
    ),
    CROUCH_IDLE(atlasKey = "crouching_idle"),
    CROUCH_WALK(atlasKey = "crouching_walking"),
    HIT(
        atlasKey = "hit",
        PlayMode.NORMAL,
    ),
    DYING(
        atlasKey = "dying",
        PlayMode.NORMAL,
    ),
}

enum class AnimationVariant(
    val atlasKey: String,
) {
    NONE(""),
    FIRST("01"),
    SECOND("02"),
    THIRD("03"),
}
