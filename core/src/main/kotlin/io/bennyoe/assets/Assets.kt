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
    WORLD_OBJECTS_ATLAS("world_objects.atlas"),
    CLOUDS_ATLAS("clouds.atlas"),
    RAIN_CLOUDS_ATLAS("rain_clouds.atlas"),
}

enum class MapAssets(
    filename: String,
    directory: String = "map",
    val descriptor: AssetDescriptor<TiledMap> = AssetDescriptor("$directory/$filename", TiledMap::class.java),
) {
    TEST_MAP("testMap.tmx"),
}

enum class SoundAssets(
    files: List<String>,
    directory: String = "sound",
    val descriptor: List<AssetDescriptor<SoundBuffer>> = files.map { file -> AssetDescriptor("$directory/$file", SoundBuffer::class.java) },
) {
    // Dawn
    DAWN_ATTACK_1_SOUND(listOf("dawn/sword/sword_1.mp3")),
    DAWN_ATTACK_2_SOUND(listOf("dawn/sword/sword_2.mp3")),
    DAWN_ATTACK_3_SOUND(listOf("dawn/sword/sword_3.mp3")),
    DAWN_FOOTSTEPS_STONE(
        listOf(
            "dawn/footsteps/stone/footsteps_stone_1.mp3",
            "dawn/footsteps/stone/footsteps_stone_2.mp3",
            "dawn/footsteps/stone/footsteps_stone_3.mp3",
            "dawn/footsteps/stone/footsteps_stone_4.mp3",
        ),
    ),
    DAWN_FOOTSTEPS_GRASS(
        listOf(
            "dawn/footsteps/grass/footsteps_grass_1.mp3",
            "dawn/footsteps/grass/footsteps_grass_2.mp3",
            "dawn/footsteps/grass/footsteps_grass_3.mp3",
            "dawn/footsteps/grass/footsteps_grass_4.mp3",
            "dawn/footsteps/grass/footsteps_grass_5.mp3",
        ),
    ),
    DAWN_HIT_SOUND(
        listOf(
            "dawn/hit/hit_1.mp3",
            "dawn/hit/hit_2.mp3",
            "dawn/hit/hit_3.mp3",
        ),
    ),
    DAWN_BASH_SOUND(listOf("dawn/bash_2.mp3")),
    DAWN_DEATH_SOUND(listOf("dawn/death.mp3")),
    DAWN_JUMP_SOUND(listOf("dawn/jump.mp3")),

    // Mushroom
    MUSHROOM_FOOTSTEPS_GRASS(
        listOf(
            "mushroom/footsteps/grass/footsteps_grass_1.mp3",
            "mushroom/footsteps/grass/footsteps_grass_2.mp3",
            "mushroom/footsteps/grass/footsteps_grass_3.mp3",
            "mushroom/footsteps/grass/footsteps_grass_4.mp3",
            "mushroom/footsteps/grass/footsteps_grass_5.mp3",
            "mushroom/footsteps/grass/footsteps_grass_6.mp3",
        ),
    ),
    MUSHROOM_FOOTSTEPS_STONE(
        listOf(
            "mushroom/footsteps/stone/footsteps_stone_1.mp3",
            "mushroom/footsteps/stone/footsteps_stone_2.mp3",
            "mushroom/footsteps/stone/footsteps_stone_3.mp3",
            "mushroom/footsteps/stone/footsteps_stone_4.mp3",
            "mushroom/footsteps/stone/footsteps_stone_5.mp3",
            "mushroom/footsteps/stone/footsteps_stone_6.mp3",
            "mushroom/footsteps/stone/footsteps_stone_7.mp3",
        ),
    ),
    MUSHROOM_HIT_SOUND(listOf("mushroom/hit_1.mp3", "mushroom/hit_2.mp3")),
    MUSHROOM_JUMP_SOUND(listOf("mushroom/jump.mp3")),
    MUSHROOM_ALARMED_SOUND(listOf("mushroom/alarmed.mp3")),
    MUSHROOM_DEATH_SOUND(listOf("mushroom/death.mp3")),
    MUSHROOM_ATTACK_SOUND(listOf("mushroom/attack.mp3")),

    // Environment
    CAMPFIRE(listOf("environment/campfire_3.mp3")),

    // Environment
    LAUGH(listOf("trigger/laugh.mp3")),
}
