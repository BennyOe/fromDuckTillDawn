package io.bennyoe.assets

import com.badlogic.gdx.graphics.g2d.TextureAtlas

data class TextureAtlases(
    val diffuseAtlas: TextureAtlas,
    val normalAtlas: TextureAtlas? = null,
    val specularAtlas: TextureAtlas? = null,
)
