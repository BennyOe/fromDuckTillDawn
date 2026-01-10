package io.bennyoe.components.animation

import com.badlogic.gdx.graphics.g2d.Animation.PlayMode

enum class SpectorAnimation(
    override val atlasKey: String,
    override val playMode: PlayMode = PlayMode.LOOP,
    override val speed: Float = 1 / 8f,
    override val normalMap: Boolean = true,
    override val specularMap: Boolean = true,
) : AnimationKey {
    IDLE("idle01"),
    WALK("walking01"),
    JUMP("jump01", playMode = PlayMode.LOOP),
    ATTACK_1("attack01", PlayMode.NORMAL, speed = 1 / 14f),
    HIT("idle01", PlayMode.NORMAL),
    DYING("dying01", PlayMode.NORMAL),
}
