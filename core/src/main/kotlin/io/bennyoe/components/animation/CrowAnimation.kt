package io.bennyoe.components.animation

import com.badlogic.gdx.graphics.g2d.Animation.PlayMode

enum class CrowAnimation(
    override val atlasKey: String,
    override val playMode: PlayMode = PlayMode.LOOP,
    override val speed: Float = 1 / 8f,
    override val normalMap: Boolean = true,
    override val specularMap: Boolean = true,
) : AnimationKey {
    FLY(atlasKey = "fly01"),
}
