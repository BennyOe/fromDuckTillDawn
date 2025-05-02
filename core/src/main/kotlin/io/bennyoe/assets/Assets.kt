package io.bennyoe.assets

import com.badlogic.gdx.assets.AssetDescriptor
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.maps.tiled.TiledMap

enum class TextureAssets(
    filename: String,
    directory: String = "textures",
    val descriptor: AssetDescriptor<TextureAtlas> = AssetDescriptor("$directory/$filename", TextureAtlas::class.java),
) {
    PLAYER_ATLAS("player.atlas"),
}

enum class MapAssets(
    filename: String,
    directory: String = "map",
    val descriptor: AssetDescriptor<TiledMap> = AssetDescriptor("$directory/$filename", TiledMap::class.java),
) {
    TEST_MAP("testMap.tmx"),
}
