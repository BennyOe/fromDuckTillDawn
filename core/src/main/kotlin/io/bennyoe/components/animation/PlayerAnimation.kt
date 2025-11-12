package io.bennyoe.components.animation

import com.badlogic.gdx.graphics.g2d.Animation.PlayMode

enum class PlayerAnimation(
    override val atlasKey: String,
    override val playMode: PlayMode = PlayMode.LOOP,
    override val speed: Float = 1 / 8f,
    override val normalMap: Boolean = true,
    override val specularMap: Boolean = true,
) : AnimationKey {
    IDLE("idle01"),
    WALK("walking01"),
    JUMP("jump01", playMode = PlayMode.LOOP),
    SWIM("swim01", playMode = PlayMode.LOOP),
    ATTACK_1("attack01", PlayMode.NORMAL, speed = 1 / 14f),
    ATTACK_2("attack02", PlayMode.NORMAL, speed = 1 / 14f),
    ATTACK_3("attack03", PlayMode.NORMAL, speed = 1 / 14f),
    BASH("bash01", PlayMode.NORMAL, speed = 1 / 20f),
    CROUCH_IDLE("crouching_idle01"),
    CROUCH_WALK("crouching_walking01"),
    HIT("hit01", PlayMode.NORMAL),
    DYING("dying01", PlayMode.NORMAL),
}
