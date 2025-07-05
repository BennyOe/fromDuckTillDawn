package io.bennyoe.components

import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

class ShaderRenderingComponent : Component<ShaderRenderingComponent> {
    var diffuse: TextureAtlas.AtlasRegion? = null
    var normal: TextureAtlas.AtlasRegion? = null
    var specular: TextureAtlas.AtlasRegion? = null

    override fun type() = ShaderRenderingComponent

    companion object : ComponentType<ShaderRenderingComponent>()
}
