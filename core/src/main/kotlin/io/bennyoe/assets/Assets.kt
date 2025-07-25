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
    // Dawn
    DAWN_ATTACK_1_SOUND("dawn/sword.mp3"),
    DAWN_ATTACK_2_SOUND("dawn/sword_02.mp3"),
    DAWN_ATTACK_3_SOUND("dawn/sword_03.mp3"),
    DAWN_FOOTSTEPS_STONE("dawn/footsteps_stone.mp3"),
    DAWN_FOOTSTEPS_WOOD("dawn/footsteps_wood.mp3"),
    DAWN_FOOTSTEPS_GRASS("dawn/footsteps_grass.mp3"),
    DAWN_HIT_SOUND("dawn/hit.mp3"),
    DAWN_BASH_SOUND("dawn/bash.mp3"),
    DAWN_DEATH_SOUND("dawn/death.mp3"),
    DAWN_JUMP_SOUND("dawn/jump.mp3"),

    // Mushroom
    MUSHROOM_FOOTSTEPS_GRASS("mushroom/footsteps_grass.mp3"),
    MUSHROOM_FOOTSTEPS_WOOD("mushroom/footsteps_wood.mp3"),
    MUSHROOM_FOOTSTEPS_STONE("mushroom/footsteps_stone.mp3"),
    MUSHROOM_HIT_SOUND("mushroom/hit.mp3"),

    // Environment
    CAMPFIRE("environment/campfire_01.mp3"),

    // Environment
    LAUGH("trigger/laugh.mp3"),
}
