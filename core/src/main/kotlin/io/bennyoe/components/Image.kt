package io.bennyoe.components

import com.badlogic.gdx.graphics.g2d.Sprite
import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

data class Image(val sprite: Sprite) : Component<Image>{
    override fun type(): ComponentType<Image> = Image
    companion object : ComponentType<Image>()
}
