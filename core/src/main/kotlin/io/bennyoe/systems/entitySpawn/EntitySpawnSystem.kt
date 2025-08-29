package io.bennyoe.systems.entitySpawn

import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.maps.MapLayer
import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World
import io.bennyoe.assets.TextureAtlases
import io.bennyoe.components.SpawnComponent
import io.bennyoe.event.MapChangedEvent
import io.bennyoe.lightEngine.core.Scene2dLightEngine
import io.bennyoe.systems.PausableSystem
import io.bennyoe.systems.debug.DefaultDebugRenderService
import io.bennyoe.utility.findLayerDeep

class EntitySpawnSystem(
    stage: Stage = World.inject("stage"),
    phyWorld: com.badlogic.gdx.physics.box2d.World = World.inject("phyWorld"),
    lightEngine: Scene2dLightEngine = World.inject("lightEngine"),
    debugRenderService: DefaultDebugRenderService = World.inject("debugRenderService"),
    worldObjectsAtlas: TextureAtlas = World.inject("worldObjectsAtlas"),
    dawnAtlases: TextureAtlases = World.inject("dawnAtlases"),
    mushroomAtlases: TextureAtlases = World.inject("mushroomAtlases"),
) : IteratingSystem(World.family { all(SpawnComponent) }),
    EventListener,
    PausableSystem {
    private val lightSpawner = LightSpawner(lightEngine)
    private val audioSpawner = AudioSpawner(world, phyWorld)
    private val skySpawner = SkySpawner(world, lightEngine, stage, worldObjectsAtlas)
    private val mapObjectSpawner = MapObjectSpawner(world, stage)
    private val characterSpawner = CharacterSpawner(world, phyWorld, lightEngine, stage, debugRenderService, dawnAtlases, mushroomAtlases)

    override fun onTickEntity(entity: Entity) {
    }

    override fun handle(event: Event): Boolean {
        when (event) {
            is MapChangedEvent -> {
                // Adding Player
                event.map.layers
                    .findLayerDeep("playerStart")
                    ?.let { characterSpawner.spawnCharacterObjects(it, getLayerZIndex(it) ?: 9000) }
                // Adding enemies
                event.map.layers
                    .findLayerDeep("enemies")
                    ?.let { characterSpawner.spawnCharacterObjects(it, getLayerZIndex(it) ?: 9000) }
                // Adding all map objects (also animated ones)
                event.map.layers
                    .findLayerDeep("bgMapObjects")
                    ?.let { mapObjectSpawner.spawnMapObjects(it, getLayerZIndex(it) ?: 8000) }
                // Adding all sky objects
                event.map.layers
                    .findLayerDeep("sky")
                    ?.let { skySpawner.spawnSkyObjects(it, getLayerZIndex(it) ?: 1000) }
                // Adding SoundEffects
                event.map.layers
                    .findLayerDeep("reverbZones")
                    ?.let { audioSpawner.spawnReverbZones(it) }
                // Adding AmbienceSounds
                event.map.layers
                    .findLayerDeep("ambienceZones")
                    ?.let { audioSpawner.spawnAmbienceZones(it) }
                // Adding SoundTriggers
                event.map.layers
                    .findLayerDeep("soundTriggers")
                    ?.let { audioSpawner.spawnSoundTriggers(it) }
                // Adding lights
                event.map.layers
                    .findLayerDeep("lights")
                    ?.let { lightSpawner.spawnFromMap(it) }

                return true
            }
        }
        return false
    }

    private fun getLayerZIndex(playerEntityLayer: MapLayer): Int? = playerEntityLayer.properties.get("zIndex") as Int?
}
