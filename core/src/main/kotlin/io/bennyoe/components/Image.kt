package io.bennyoe.components

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

data class Image(val region: TextureRegion) : Component<Image>{
    override fun type(): ComponentType<Image> = Image
    companion object : ComponentType<Image>()
}
