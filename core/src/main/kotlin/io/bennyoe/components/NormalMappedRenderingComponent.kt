package io.bennyoe.components

import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

class NormalMappedRenderingComponent : Component<NormalMappedRenderingComponent> {
    var diffuse: TextureAtlas.AtlasRegion? = null
    var normal: TextureAtlas.AtlasRegion? = null

    override fun type() = NormalMappedRenderingComponent

    companion object : ComponentType<NormalMappedRenderingComponent>()
}
