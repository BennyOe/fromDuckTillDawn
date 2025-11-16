package io.bennyoe.components.animation

import com.badlogic.gdx.graphics.g2d.Animation.PlayMode

enum class MinotaurAnimation(
    override val atlasKey: String,
    override val playMode: PlayMode = PlayMode.LOOP,
    override val speed: Float = 1 / 8f,
    override val normalMap: Boolean = true,
    override val specularMap: Boolean = true,
) : AnimationKey {
    IDLE("idle01"),
    WALK("walking01"),
    SCREAM("scream01"),
    SPIN_ATTACK_START("spinAttackStart01"),
    SPIN_ATTACK_LOOP("spinAttackLoop01"),
    SPIN_ATTACK_STOP("spinAttackStop01"),
    SHAKING_PLAYER("shake01", speed = 0.5f),
    STUNNED("stunned01", speed = 0.5f),
    ROCK_ATTACK("rockAttack01", speed = 0.5f),
    STOMP_ATTACK("stompAttack01", speed = 0.5f),
    THROW_PLAYER("throw01", speed = 0.5f),
    HIT("hit01", PlayMode.NORMAL),
    DYING("dying01", PlayMode.NORMAL),
    ROCK_BREAK("rockBreak01", PlayMode.NORMAL, speed = 0.5f),
}
