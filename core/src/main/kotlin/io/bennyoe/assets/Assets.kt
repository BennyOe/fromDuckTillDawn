package io.bennyoe.assets

import com.badlogic.gdx.assets.AssetDescriptor
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.maps.tiled.TiledMap
import de.pottgames.tuningfork.SoundBuffer

enum class TextureAssets(
    filename: String,
    directory: String = "textures",
    val descriptor: AssetDescriptor<TextureAtlas> = AssetDescriptor("$directory/$filename", TextureAtlas::class.java),
) {
    DAWN_ATLAS("dawn.atlas"),
    DAWN_N_ATLAS("dawn_n.atlas"),
    DAWN_S_ATLAS("dawn_s.atlas"),
    MUSHROOM_ATLAS("mushroom.atlas"),
    MUSHROOM_N_ATLAS("mushroom_n.atlas"),
    MUSHROOM_S_ATLAS("mushroom_s.atlas"),
    PARTICLE_ATLAS("particles.atlas"),
}

enum class MapAssets(
    filename: String,
    directory: String = "map",
    val descriptor: AssetDescriptor<TiledMap> = AssetDescriptor("$directory/$filename", TiledMap::class.java),
) {
    TEST_MAP("testMap.tmx"),
}

enum class SoundAssets(
    filename: String,
    directory: String = "sound",
    val descriptor: AssetDescriptor<SoundBuffer> = AssetDescriptor("$directory/$filename", SoundBuffer::class.java),
) {
    ATTACK_SOUND("sword.mp3"),
    WALK_SOUND("footsteps.mp3"),
    HIT_SOUND("hit.mp3"),
}
