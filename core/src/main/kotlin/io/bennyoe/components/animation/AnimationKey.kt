package io.bennyoe.components.animation

import com.badlogic.gdx.graphics.g2d.Animation.PlayMode

interface AnimationKey {
    val atlasKey: String
    val playMode: PlayMode
    val speed: Float
    val normalMap: Boolean
    val specularMap: Boolean
}

data object NoAnimationKey : AnimationKey {
    override val atlasKey: String = ""
    override val playMode: PlayMode = PlayMode.LOOP
    override val speed: Float = 1f
    override val normalMap: Boolean = false
    override val specularMap: Boolean = false
}
