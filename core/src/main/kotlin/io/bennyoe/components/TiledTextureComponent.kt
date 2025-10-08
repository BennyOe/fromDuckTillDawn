package io.bennyoe.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

class TiledTextureComponent(
    val scale: Float = 1f,
) : Component<TiledTextureComponent> {
    override fun type() = TiledTextureComponent

    companion object : ComponentType<TiledTextureComponent>()
}
