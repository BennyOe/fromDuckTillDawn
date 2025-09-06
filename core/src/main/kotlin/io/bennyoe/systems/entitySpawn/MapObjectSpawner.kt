package io.bennyoe.systems.entitySpawn

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.maps.MapLayer
import com.badlogic.gdx.maps.tiled.objects.TiledMapTileMapObject
import com.badlogic.gdx.maps.tiled.tiles.AnimatedTiledMapTile
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.github.quillraven.fleks.World
import io.bennyoe.components.AnimationComponent
import io.bennyoe.components.ImageComponent
import io.bennyoe.components.ParticleComponent
import io.bennyoe.components.ParticleType
import io.bennyoe.components.TransformComponent
import io.bennyoe.components.audio.AudioComponent
import io.bennyoe.config.GameConstants
import io.bennyoe.systems.audio.SoundType
import ktx.math.vec2
import ktx.tiled.type

class MapObjectSpawner(
    val world: World,
    val stage: Stage,
) {
    fun spawnMapObjects(
        mapObjectsLayer: MapLayer,
        layerZIndex: Int,
    ) {
        mapObjectsLayer.objects.forEach { mapObject ->
            mapObject as TiledMapTileMapObject
            world.entity {
                val zIndex = mapObject.properties.get("zIndex", Int::class.java) ?: 0
                val image = ImageComponent(stage, zIndex = layerZIndex + zIndex)
                image.image = Image(mapObject.tile.textureRegion)
                val width =
                    mapObject.tile.textureRegion.regionWidth
                        .toFloat() * GameConstants.UNIT_SCALE
                val height =
                    mapObject.tile.textureRegion.regionHeight
                        .toFloat() * GameConstants.UNIT_SCALE
                it += image

                if (mapObject.properties.get("sound") != null) {
                    it +=
                        AudioComponent(
                            SoundType.valueOf(mapObject.properties.get("sound", String::class.java).uppercase()),
                            mapObject.properties.get("soundVolume", Float::class.java) ?: .5f,
                            mapObject.properties.get("soundAttenuationMaxDistance", Float::class.java) ?: 10f,
                            mapObject.properties.get("soundAttenuationMinDistance", Float::class.java) ?: 1f,
                            mapObject.properties.get("soundAttenuationFactor", Float::class.java) ?: 1f,
                        )
                }

                if (mapObject.tile is AnimatedTiledMapTile) {
                    val animatedTile = mapObject.tile as AnimatedTiledMapTile
                    val frameInterval = 64f / 1000f
                    val frames = animatedTile.frameTiles.map { tile -> TextureRegionDrawable(tile.textureRegion) }
                    val animation = Animation(frameInterval, *frames.toTypedArray())
                    animation.playMode = Animation.PlayMode.LOOP

                    val aniCmp = AnimationComponent()
                    aniCmp.animation = animation
                    it += aniCmp
                }

                it +=
                    TransformComponent(
                        vec2(mapObject.x * GameConstants.UNIT_SCALE, mapObject.y * GameConstants.UNIT_SCALE),
                        width,
                        height,
                    )
                // Add ParticleComponent for fire if the map object type is "fire"
                if (mapObject.type == "fire") {
                    it +=
                        ParticleComponent(
                            particleFile = Gdx.files.internal("particles/fire.p"),
                            scaleFactor = 1f / 82f,
                            motionScaleFactor = 1f / 50f,
                            looping = true,
                            offsetX = width * 0.5f,
                            offsetY = 0.2f,
                            stage = stage,
                            type = ParticleType.FIRE,
                        )
                }
            }
        }
    }
}
