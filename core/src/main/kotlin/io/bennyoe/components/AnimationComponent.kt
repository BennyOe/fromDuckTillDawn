package io.bennyoe.components

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
    var currentAnimationType: AnimationType = AnimationType.NONE,
    var animationSoundTriggers: Map<AnimationType, Map<Int, SoundType>> = emptyMap(),
) : Component<AnimationComponent> {
    override fun type() = AnimationComponent

    lateinit var animation: Animation<TextureRegionDrawable>
    var nextAnimationModel: AnimationModel = AnimationModel.NONE
        private set
    var nextAnimationType: AnimationType = AnimationType.NONE
        private set

    var previousFrameIndex: Int = -1

    fun nextAnimation(type: AnimationType) {
        nextAnimationModel = animationModel
        nextAnimationType = type
    }

    fun clearAnimation() {
        nextAnimationModel = AnimationModel.NONE
        nextAnimationType = AnimationType.NONE
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
    IDLE("idle01"),
    WALK(
        "walking01",
    ),
    JUMP(
        atlasKey = "jump01",
        playMode = PlayMode.LOOP,
    ),
    SWIM(
        atlasKey = "swim01",
        playMode = PlayMode.LOOP,
    ),
    ATTACK_1(
        atlasKey = "attack01",
        PlayMode.NORMAL,
        speed = 1 / 14f,
    ),
    ATTACK_2(
        atlasKey = "attack02",
        PlayMode.NORMAL,
        speed = 1 / 14f,
    ),
    ATTACK_3(
        atlasKey = "attack03",
        PlayMode.NORMAL,
        speed = 1 / 14f,
    ),
    BASH(
        atlasKey = "bash01",
        PlayMode.NORMAL,
        speed = 1 / 20f,
    ),
    CROUCH_IDLE(atlasKey = "crouching_idle01"),
    CROUCH_WALK(atlasKey = "crouching_walking01"),
    HIT(
        atlasKey = "hit01",
        PlayMode.NORMAL,
    ),
    DYING(
        atlasKey = "dying01",
        PlayMode.NORMAL,
    ),
}
