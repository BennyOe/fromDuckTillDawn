package io.bennyoe.systems.entitySpawn

import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.maps.MapLayer
import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World
import com.github.quillraven.fleks.World.Companion.inject
import io.bennyoe.assets.TextureAtlases
import io.bennyoe.components.SpawnComponent
import io.bennyoe.event.MapChangedEvent
import io.bennyoe.lightEngine.core.Scene2dLightEngine
import io.bennyoe.systems.PausableSystem
import io.bennyoe.systems.debug.DefaultDebugRenderService
import io.bennyoe.systems.render.ZIndex
import io.bennyoe.utility.findLayerDeep
import io.bennyoe.utility.findLayersStartingWithDeep

class EntitySpawnSystem(
    stage: Stage = inject("stage"),
    uiStage: Stage = inject("uiStage"),
    phyWorld: com.badlogic.gdx.physics.box2d.World = inject("phyWorld"),
    lightEngine: Scene2dLightEngine = inject("lightEngine"),
    debugRenderService: DefaultDebugRenderService = inject("debugRenderService"),
    worldObjectsAtlas: TextureAtlas = inject("worldObjectsAtlas"),
    dawnAtlases: TextureAtlases = inject("dawnAtlases"),
    mushroomAtlases: TextureAtlases = inject("mushroomAtlases"),
    minotaurAtlases: TextureAtlases = inject("minotaurAtlases"),
    spectorAtlases: TextureAtlases = inject("spectorAtlases"),
    bgNormalAtlases: TextureAtlases = inject("bgNormalAtlases"),
    foregroundAtlas: TextureAtlas = inject("foregroundAtlas"),
    animatedBgAtlas: TextureAtlas = inject("animatedBgAtlas"),
) : IteratingSystem(World.family { all(SpawnComponent) }),
    EventListener,
    PausableSystem {
    private val lightSpawner = LightSpawner(world, lightEngine)
    private val audioSpawner = AudioSpawner(world, phyWorld)
    private val skySpawner = SkySpawner(world, lightEngine, stage, worldObjectsAtlas)
    private val mapObjectSpawner = MapObjectSpawner(world, stage, phyWorld, lightEngine, worldObjectsAtlas)
    private val characterSpawner =
        CharacterSpawner(
            world = world,
            phyWorld = phyWorld,
            lightEngine = lightEngine,
            stage = stage,
            uiStage = uiStage,
            debugRenderer = debugRenderService,
            dawnAtlases = dawnAtlases,
            mushroomAtlases = mushroomAtlases,
            minotaurAtlases = minotaurAtlases,
            spectorAtlases = spectorAtlases,
        )
    private val rainMaskSpawner = RainMaskSpawner(world, stage)
    private val waterSpawner = WaterSpawner(world, phyWorld)
    private val bgNormalSpawner = BgNormalSpawner(world, stage, bgNormalAtlases)
    private val fadingForegroundSpawner = FadingForegroundSpawner(world, stage, foregroundAtlas)
    private val doorSpawner = DoorSpawner(world, stage, phyWorld, lightEngine, worldObjectsAtlas)
    private val backgroundParallaxSpawner = BackgroundParallaxSpawner(world, stage, animatedBgAtlas)

    override fun onTickEntity(entity: Entity) {
    }

    override fun handle(event: Event): Boolean {
        when (event) {
            is MapChangedEvent -> {
                // Adding Player
                event.map.layers
                    .findLayerDeep("playerStart")
                    ?.let {
                        characterSpawner.spawnCharacterObjects(
                            it,
                            getLayerZIndex(it)
                                ?: ZIndex.CHARACTERS.value,
                        )
                    }
                // Adding enemies
                event.map.layers
                    .findLayerDeep("enemies")
                    ?.let {
                        characterSpawner.spawnCharacterObjects(
                            it,
                            getLayerZIndex(it)
                                ?: ZIndex.CHARACTERS.value,
                        )
                    }
                // Adding all map objects (also animated ones)
                event.map.layers
                    .findLayerDeep("bgMapObjects")
                    ?.let {
                        mapObjectSpawner.spawnMapObjects(
                            it,
                            getLayerZIndex(it)
                                ?: ZIndex.BG_OBJECTS.value,
                        )
                    }
                // Adding all sky objects
                event.map.layers
                    .findLayerDeep("sky")
                    ?.let { skySpawner.spawnSkyObjects(it, getLayerZIndex(it) ?: ZIndex.SKY.value) }
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
                // Adding rain masks
                event.map.layers
                    .findLayerDeep("rainMask")
                    ?.let { rainMaskSpawner.spawnRainMasks(it) }
                // Adding water
                event.map.layers
                    .findLayerDeep("water")
                    ?.let { waterSpawner.spawnWater(it) }
                // Adding backgrounds with normal maps
                event.map.layers
                    .findLayerDeep("bgNormal")
                    ?.let {
                        bgNormalSpawner.spawnBgNormal(
                            it,
                            getLayerZIndex(it)
                                ?: ZIndex.BG_WITH_NORMALS.value,
                        )
                    }
                // Adding forground Images
                event.map.layers
                    .findLayerDeep("fadingForeground")
                    ?.let {
                        fadingForegroundSpawner.spawnForeground(
                            it,
                            getLayerZIndex(it)
                                ?: ZIndex.FADING_FOREGROUND.value,
                        )
                    }
                // Adding doors
                event.map.layers
                    .findLayerDeep("doors")
                    ?.let { doorSpawner.spawnDoors(it, getLayerZIndex(it) ?: ZIndex.DOORS.value) }
                // Adding door triggers
                event.map.layers
                    .findLayerDeep("triggers")
                    ?.let { doorSpawner.spawnTrigger(it) }
                event.map.layers
                    .findLayersStartingWithDeep("parallax")
                    .forEach { layer ->
                        backgroundParallaxSpawner.spawnParallaxBackgrounds(
                            layer,
                            getLayerZIndex(layer) ?: 0,
                        )
                    }
                return true
            }
        }
        return false
    }

    private fun getLayerZIndex(playerEntityLayer: MapLayer): Int? = playerEntityLayer.properties.get("zIndex") as Int?
}
