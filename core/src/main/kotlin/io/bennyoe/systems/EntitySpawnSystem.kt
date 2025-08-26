package io.bennyoe.systems

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ai.msg.MessageManager
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.maps.MapLayer
import com.badlogic.gdx.maps.MapObject
import com.badlogic.gdx.maps.tiled.objects.TiledMapTileMapObject
import com.badlogic.gdx.maps.tiled.tiles.AnimatedTiledMapTile
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.physics.box2d.World
import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.GdxRuntimeException
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import com.github.quillraven.fleks.World.Companion.inject
import io.bennyoe.PlayerInputProcessor
import io.bennyoe.ai.blackboards.MushroomContext
import io.bennyoe.assets.TextureAtlases
import io.bennyoe.components.AnimationComponent
import io.bennyoe.components.AnimationModel
import io.bennyoe.components.AnimationType
import io.bennyoe.components.AttackComponent
import io.bennyoe.components.DeadComponent
import io.bennyoe.components.GameStateComponent
import io.bennyoe.components.GroundTypeSensorComponent
import io.bennyoe.components.HealthComponent
import io.bennyoe.components.ImageComponent
import io.bennyoe.components.InputComponent
import io.bennyoe.components.IntentionComponent
import io.bennyoe.components.JumpComponent
import io.bennyoe.components.LightComponent
import io.bennyoe.components.MoveComponent
import io.bennyoe.components.ParticleComponent
import io.bennyoe.components.PhysicComponent
import io.bennyoe.components.PlayerComponent
import io.bennyoe.components.ShaderRenderingComponent
import io.bennyoe.components.SkyComponent
import io.bennyoe.components.SkyComponentType
import io.bennyoe.components.SpawnComponent
import io.bennyoe.components.StateComponent
import io.bennyoe.components.TransformComponent
import io.bennyoe.components.ai.BasicSensorsComponent
import io.bennyoe.components.ai.BehaviorTreeComponent
import io.bennyoe.components.ai.NearbyEnemiesComponent
import io.bennyoe.components.ai.RayHitComponent
import io.bennyoe.components.audio.AmbienceSoundComponent
import io.bennyoe.components.audio.AudioComponent
import io.bennyoe.components.audio.ReverbZoneComponent
import io.bennyoe.components.audio.ReverbZoneContactComponent
import io.bennyoe.components.audio.SoundProfileComponent
import io.bennyoe.components.audio.SoundTriggerComponent
import io.bennyoe.config.EntityCategory
import io.bennyoe.config.GameConstants.UNIT_SCALE
import io.bennyoe.config.SpawnCfg
import io.bennyoe.event.MapChangedEvent
import io.bennyoe.lightEngine.core.LightEffectType
import io.bennyoe.lightEngine.core.Scene2dLightEngine
import io.bennyoe.service.DefaultDebugRenderService
import io.bennyoe.service.SoundType
import io.bennyoe.state.FsmMessageTypes
import io.bennyoe.state.mushroom.MushroomCheckAliveState
import io.bennyoe.state.mushroom.MushroomFSM
import io.bennyoe.state.mushroom.MushroomStateContext
import io.bennyoe.state.player.PlayerCheckAliveState
import io.bennyoe.state.player.PlayerFSM
import io.bennyoe.state.player.PlayerStateContext
import io.bennyoe.systems.audio.AmbienceType
import io.bennyoe.utility.BodyData
import io.bennyoe.utility.FixtureData
import io.bennyoe.utility.SensorType
import io.bennyoe.utility.findLayerDeep
import ktx.app.gdxError
import ktx.box2d.box
import ktx.box2d.circle
import ktx.log.logger
import ktx.math.plus
import ktx.math.times
import ktx.math.vec2
import ktx.tiled.shape
import ktx.tiled.type
import ktx.tiled.x
import ktx.tiled.y

// TODO refactor
class EntitySpawnSystem(
    private val stage: Stage = inject("stage"),
    private val phyWorld: World = inject("phyWorld"),
    private val debugRenderService: DefaultDebugRenderService = inject("debugRenderService"),
    private val lightEngine: Scene2dLightEngine = inject("lightEngine"),
    private val worldObjectsAtlas: TextureAtlas = inject("worldObjectsAtlas"),
    dawnAtlases: TextureAtlases = inject("dawnAtlases"),
    mushroomAtlases: TextureAtlases = inject("mushroomAtlases"),
) : IteratingSystem(family { all(SpawnComponent) }),
    EventListener,
    PausableSystem {
    private val atlasMap: Map<AnimationModel, TextureAtlas> =
        mapOf(
            AnimationModel.PLAYER_DAWN to dawnAtlases.diffuseAtlas,
            AnimationModel.ENEMY_MUSHROOM to mushroomAtlases.diffuseAtlas,
        )

    private val sizesCache = mutableMapOf<String, Vector2>()
    private val messageDispatcher = MessageManager.getInstance()

    override fun onTickEntity(entity: Entity) {
    }

    override fun handle(event: Event): Boolean {
        when (event) {
            is MapChangedEvent -> {
                // Adding Player
                val playerEntityLayer = event.map.layers.findLayerDeep("playerStart")
                playerEntityLayer?.objects?.forEach { playerObj ->
                    val cfg = SpawnCfg.createSpawnCfg(playerObj.type!!)
                    createEntity(playerObj, cfg, getLayerZindex(playerEntityLayer) ?: 9000)
                }
                // Adding enemies
                val enemyEntityLayer = event.map.layers.findLayerDeep("enemies")
                enemyEntityLayer?.objects?.forEach { enemyObj ->
                    val cfg = SpawnCfg.createSpawnCfg(enemyObj.type!!)
                    createEntity(enemyObj, cfg, getLayerZindex(enemyEntityLayer) ?: 9000)
                }
                // Adding all map objects (also animated ones)
                val bgMapObjects = event.map.layers.findLayerDeep("bgMapObjects")
                bgMapObjects?.objects?.forEach { mapObject ->
                    createMapObject(mapObject as TiledMapTileMapObject, getLayerZindex(bgMapObjects) ?: 8000)
                }
                // Adding all sky objects
                val skyLayer = event.map.layers.findLayerDeep("sky")
                skyLayer?.objects?.forEach { mapObject ->
                    createSkyObjects(mapObject, getLayerZindex(skyLayer) ?: 1000)
                }
                // Adding SoundEffects
                val audioZones = event.map.layers.findLayerDeep("reverbZones")
                audioZones?.objects?.forEach { audioZoneObj ->
                    createReverbZone(audioZoneObj)
                }
                // Adding AmbienceSounds
                val ambienceSounds = event.map.layers.findLayerDeep("ambienceZones")
                ambienceSounds?.objects?.forEach { ambienceSoundObj ->
                    createAmbienceSounds(ambienceSoundObj)
                }
                // Adding SoundTriggers
                val soundTriggers = event.map.layers.findLayerDeep("soundTriggers")
                soundTriggers?.objects?.forEach { soundTriggerObj ->
                    createSoundTrigger(soundTriggerObj)
                }
                // Adding lights
                val lightsLayer = event.map.layers.findLayerDeep("lights")
                lightsLayer?.objects?.forEach { light ->
                    val type = LightType.entries[(light.properties.get("type") as Int)]
                    val position = vec2(light.x, light.y)
                    val color = light.properties.get("color") as Color
                    val initialIntensity = light.properties.get("initialIntensity") as Float? ?: 1f
                    val b2dDistance = light.properties.get("distance") as Float? ?: 1f
                    val falloffProfile = light.properties.get("falloffProfile") as Float? ?: 0.5f
                    val shaderIntensityMultiplier = light.properties.get("shaderIntensityMultiplier") as Float? ?: 0.5f
                    val isManaged = light.properties.get("isManaged") as Boolean? ?: true

                    // spotlight specific
                    val direction = light.properties.get("direction") as Float? ?: -90f
                    val coneDegree = light.properties.get("coneDegree") as Float? ?: 50f

                    val effect = (light.properties.get("effect") as? Int)?.let { LightEffectType.entries[it] }

                    createLight(
                        type,
                        position,
                        color,
                        initialIntensity,
                        b2dDistance,
                        falloffProfile,
                        shaderIntensityMultiplier,
                        effect,
                        direction,
                        coneDegree,
                        isManaged,
                    )
                }
                return true
            }
        }
        return false
    }

    private fun getLayerZindex(playerEntityLayer: MapLayer): Int? = playerEntityLayer.properties.get("zIndex") as Int?

    private fun createReverbZone(audioZoneObj: MapObject) {
        world.entity {
            val physicCmp =
                PhysicComponent.physicsComponentFromShape2D(
                    phyWorld,
                    audioZoneObj.shape,
                    isSensor = true,
                    sensorType = SensorType.AUDIO_EFFECT_SENSOR,
                    setUserData = BodyData(EntityCategory.SENSOR, it),
                    categoryBit = EntityCategory.SENSOR.bit,
                )
            it += physicCmp
            it +=
                ReverbZoneComponent(
                    audioZoneObj.properties.get("effectPreset", String::class.java),
                    audioZoneObj.properties.get("effectIntensity", Float::class.java),
                )
        }
    }

    private fun createSoundTrigger(soundTriggerObj: MapObject) {
        world.entity {
            val physicCmp =
                PhysicComponent.physicsComponentFromShape2D(
                    phyWorld,
                    soundTriggerObj.shape,
                    isSensor = true,
                    sensorType = SensorType.SOUND_TRIGGER_SENSOR,
                    setUserData = BodyData(EntityCategory.SENSOR, it),
                    categoryBit = EntityCategory.SENSOR.bit,
                )
            it += physicCmp
            it +=
                SoundTriggerComponent(
                    soundTriggerObj.properties.get("sound") as String?,
                    (soundTriggerObj.properties.get("type") as String?)?.uppercase()?.let { type -> SoundType.valueOf(type) },
                    soundTriggerObj.properties.get("streamed", Boolean::class.java),
                    soundTriggerObj.properties.get("volume", Float::class.java),
                )
        }
    }

    private fun createAmbienceSounds(soundTriggerObj: MapObject) {
        world.entity {
            val physicCmp =
                PhysicComponent.physicsComponentFromShape2D(
                    phyWorld,
                    soundTriggerObj.shape,
                    isSensor = true,
                    sensorType = SensorType.SOUND_AMBIENCE_SENSOR,
                    setUserData = BodyData(EntityCategory.SENSOR, it),
                    categoryBit = EntityCategory.SENSOR.bit,
                )
            it += physicCmp
            it +=
                AmbienceSoundComponent(
                    AmbienceType.valueOf((soundTriggerObj.properties.get("type") as String).uppercase()),
                    soundTriggerObj.properties.get("sound") as String,
                    soundTriggerObj.properties.get("volume") as? Float,
                )
        }
    }

    private fun createMapObject(
        mapObject: TiledMapTileMapObject,
        layerZIndex: Int,
    ) {
        world.entity {
            val zIndex = mapObject.properties.get("zIndex", Int::class.java) ?: 0
            val image = ImageComponent(stage, zIndex = layerZIndex + zIndex)
            image.image = Image(mapObject.tile.textureRegion)
            val width =
                mapObject.tile.textureRegion.regionWidth
                    .toFloat() * UNIT_SCALE
            val height =
                mapObject.tile.textureRegion.regionHeight
                    .toFloat() * UNIT_SCALE
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
                    vec2(mapObject.x * UNIT_SCALE, mapObject.y * UNIT_SCALE),
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
                    )
            }
        }
    }

    private fun createSkyObjects(
        mapObject: MapObject,
        layerZIndex: Int,
    ) {
        val zIndex = mapObject.properties.get("zIndex", Int::class.java) ?: 0
        val width = (stage.camera as OrthographicCamera).viewportWidth
        val height = (stage.camera as OrthographicCamera).viewportHeight
        when (mapObject.type) {
            "shootingStar" -> {
                world.entity {
                    it += TransformComponent(vec2(0f, 0f), width, height)
                    val particle =
                        ParticleComponent(
                            particleFile = Gdx.files.internal("particles/shootingStar.p"),
                            scaleFactor = 0.1f,
                            motionScaleFactor = 0.2f,
                            looping = true,
                            stage = stage,
                            zIndex = layerZIndex + zIndex,
                        )
                    it += particle
                    it += SkyComponent(SkyComponentType.SHOOTING_STAR)
                }
            }

            "sky", "stars" -> {
                world.entity {
                    val imageCmp = ImageComponent(stage, zIndex = layerZIndex + zIndex)
                    val imageName = mapObject.properties.get("image") as String
                    imageCmp.image = Image(worldObjectsAtlas.findRegion(imageName))

                    it += imageCmp
                    it += TransformComponent(vec2(0f, 0f), width, height)

                    val skyType = if (mapObject.type == "sky") SkyComponentType.SKY else SkyComponentType.STARS
                    it += SkyComponent(skyType)
                }
            }

            "moon" -> {
                world.entity {
                    it += SkyComponent(SkyComponentType.MOON)
                    val image = ImageComponent(stage, zIndex = layerZIndex + zIndex)
                    image.image = Image(worldObjectsAtlas.findRegion("moon2"))
                    it += image
                    val transform = TransformComponent(vec2(0f, 0f), 3f, 3f)
                    it += transform
                    val moonLight =
                        LightComponent(
                            lightEngine.addPointLight(
                                position = transform.position,
                                color = Color.WHITE,
                                b2dDistance = 9f,
                                isManaged = false,
                            ),
                        )
                    moonLight.gameLight.b2dLight.isXray = true
                    moonLight.gameLight.b2dLight.isStaticLight = false
                    it += moonLight
                    val shaderRenderingCmp = ShaderRenderingComponent()
                    shaderRenderingCmp.shader = setupShader("moon")
                    shaderRenderingCmp.uniforms.putAll(
                        mapOf(
                            "u_halo_color" to Vector3(1f, 1f, 1f),
                            "u_halo_radius" to 0.13f,
                            "u_halo_falloff" to 0.42f,
                            "u_halo_strength" to 0.4f,
                        ),
                    )
                    it += shaderRenderingCmp
                }
            }

            "sun" -> {
                world.entity {
                    it += SkyComponent(SkyComponentType.SUN)
                    val image = ImageComponent(stage, zIndex = layerZIndex + zIndex)
                    image.image = Image(worldObjectsAtlas.findRegion("sun2"))
                    it += image
                    val transform = TransformComponent(vec2(0f, 0f), 6f, 6f)
                    it += transform
                    val sunLight =
                        LightComponent(
                            lightEngine.addPointLight(
                                position = transform.position,
                                color = Color.ORANGE,
                                b2dDistance = 2f,
                                isManaged = false,
                            ),
                        )
                    sunLight.gameLight.b2dLight.isStaticLight = true
                    it += sunLight
                    val shaderRenderingCmp = ShaderRenderingComponent()
                    shaderRenderingCmp.shader = setupShader("sun")
                    val region = worldObjectsAtlas.findRegion("noise")
                    val tex =
                        region.texture.apply {
                            setWrap(
                                Texture.TextureWrap.Repeat,
                                Texture.TextureWrap.Repeat,
                            )
                        }
                    shaderRenderingCmp.noiseTexture = tex
                    shaderRenderingCmp.uniforms.putAll(
                        mapOf(
                            "u_noiseOffset" to Vector2(region.u, region.v),
                            "u_noiseScale" to Vector2(region.u2 - region.u, region.v2 - region.v),
                            "u_sunsetCenter" to 17.25f,
                            "u_halfWidth" to 1.25f,
                            "u_tintStrength" to 0.6f,
                            "u_sunsetTint" to Vector3(1.0f, 0.5f, 0.2f),
                            "u_halo_color" to Vector3(1.0f, 0.6f, 0.2f),
                            "u_halo_radius" to 0.13f,
                            "u_halo_falloff" to 0.42f,
                            "u_halo_strength" to 1.0f,
                            "u_shimmer_strength" to 0.03f,
                            "u_shimmer_speed" to 0.2f,
                            "u_shimmer_scale" to 1.0f,
                        ),
                    )
                    it += shaderRenderingCmp
                }
            }
        }
    }

    private fun createLight(
        type: LightType,
        position: Vector2,
        color: Color,
        initialIntensity: Float,
        b2dDistance: Float,
        falloffProfile: Float,
        shaderIntensityMultiplier: Float,
        effect: LightEffectType?,
        direction: Float,
        coneDegree: Float,
        isManaged: Boolean,
    ) {
        when (type) {
            LightType.POINT_LIGHT -> {
                val pointLight =
                    lightEngine.addPointLight(
                        position * UNIT_SCALE,
                        color,
                        initialIntensity,
                        b2dDistance,
                        falloffProfile,
                        shaderIntensityMultiplier,
                        isManaged = isManaged,
                    )
                pointLight.effect = effect
                pointLight.setOn(true)
            }

            LightType.SPOT_LIGHT -> {
                val spotLight =
                    lightEngine.addSpotLight(
                        position * UNIT_SCALE,
                        color,
                        direction,
                        coneDegree,
                        initialIntensity,
                        b2dDistance,
                        falloffProfile,
                        shaderIntensityMultiplier,
                        isManaged = isManaged,
                    )
                spotLight.effect = effect
            }
        }
    }

    private fun createEntity(
        mapObj: MapObject,
        cfg: SpawnCfg,
        layerZIndex: Int,
    ) {
        val relativeSize = size(cfg.animationModel, cfg.animationType)
        world.entity {
            // Add general components
            val image =
                // scale sets the image size
                ImageComponent(stage, cfg.scaleImage.x, cfg.scaleImage.y, zIndex = layerZIndex + cfg.zIndex).apply {
                    image =
                        Image().apply {
                            setPosition(mapObj.x * UNIT_SCALE, mapObj.y * UNIT_SCALE)
                            // this size sets the physic box
                            setSize(relativeSize.x, relativeSize.y)
                        }
                }
            it += image

            val animation = AnimationComponent()
            animation.animationModel = cfg.animationModel
            animation.nextAnimation(cfg.animationType)
            animation.animationSoundTriggers = cfg.soundTrigger
            it += animation

            val physics =
                PhysicComponent.physicsComponentFromImage(
                    phyWorld,
                    image.image,
                    cfg.bodyType,
                    categoryBit = cfg.entityCategory.bit,
                    maskBit = cfg.physicMaskCategory,
                    scalePhysicX = cfg.scalePhysic.x,
                    scalePhysicY = cfg.scalePhysic.y,
                    myFriction = 0f,
                    offsetY = cfg.offsetPhysic.y,
                    setUserdata = BodyData(cfg.entityCategory, it),
                    sensorType = SensorType.HITBOX_SENSOR,
                )
            physics.categoryBits = cfg.entityCategory.bit

            physics.body.box(
                physics.size.x * 0.99f,
                0.01f,
                Vector2(0f, 0f - physics.size.y * 0.5f) + cfg.offsetPhysic.y,
            ) {
                isSensor = true
                userData = FixtureData(SensorType.GROUND_TYPE_SENSOR)
                filter.categoryBits = EntityCategory.SENSOR.bit
                filter.maskBits = EntityCategory.GROUND.bit
            }

            it += physics

            it += GroundTypeSensorComponent

            it +=
                TransformComponent(
                    vec2(physics.body.position.x, physics.body.position.y),
                    physics.size.x,
                    physics.size.y,
                )
            it += HealthComponent()
            it +=
                DeadComponent(
                    cfg.keepCorpse,
                    cfg.removeDelay,
                    cfg.removeDelay,
                )

            val move = MoveComponent()
            move.maxSpeed *= cfg.scaleSpeed
            move.chaseSpeed = cfg.chaseSpeed
            it += move

            if (cfg.canAttack) {
                val attackCmp = AttackComponent()
                attackCmp.extraRange *= cfg.attackExtraRange
                attackCmp.maxDamage *= cfg.scaleAttackDamage
                attackCmp.attackDelay = cfg.attackDelay
                it += attackCmp
            }

            it += ShaderRenderingComponent()

            it += JumpComponent()

            it += SoundProfileComponent(cfg.soundProfile)

            when (cfg.entityCategory) {
                // Add Player specific components
                EntityCategory.PLAYER -> {
                    val phyCmp = it[PhysicComponent]
                    phyCmp.body.box(
                        physics.size.x * 0.99f,
                        0.01f,
                        Vector2(0f, 0f - physics.size.y * 0.5f),
                    ) {
                        isSensor = true
                        userData = FixtureData(SensorType.GROUND_DETECT_SENSOR)
                        filter.categoryBits = EntityCategory.SENSOR.bit
                        filter.maskBits = EntityCategory.GROUND.bit
                    }
                    val flashlight =
                        lightEngine.addSpotLight(
                            position = phyCmp.body.position,
                            color = Color.WHITE,
                            direction = 0f,
                            coneDegree = 30f,
                            initialIntensity = 1.8f,
                            b2dDistance = 12f,
                            falloffProfile = 0.4f,
                            shaderIntensityMultiplier = 0.9f,
                        )
                    flashlight.setOn(false)

                    it += LightComponent(flashlight)

                    it += ReverbZoneContactComponent()

                    val input = InputComponent()
                    it += input

                    it += IntentionComponent()

                    val player = PlayerComponent()
                    it += player

                    val state =
                        StateComponent(
                            world = world,
                            owner = PlayerStateContext(it, world, stage),
                            initialState = PlayerFSM.IDLE,
                            globalState = PlayerCheckAliveState,
                        )
                    it += state

                    messageDispatcher.addListener(state.stateMachine, FsmMessageTypes.HEAL.ordinal)
                    messageDispatcher.addListener(state.stateMachine, FsmMessageTypes.ATTACK.ordinal)
                    messageDispatcher.addListener(state.stateMachine, FsmMessageTypes.KILL.ordinal)
                    messageDispatcher.addListener(state.stateMachine, FsmMessageTypes.PLAYER_IS_HIT.ordinal)

                    PlayerInputProcessor(world = world)
                }

                // Add Enemy specific components
                EntityCategory.ENEMY -> {
                    it += IntentionComponent()

                    it += NearbyEnemiesComponent()

                    val state =
                        StateComponent(
                            world = world,
                            owner = MushroomStateContext(it, world, stage),
                            initialState = MushroomFSM.IDLE,
                            globalState = MushroomCheckAliveState,
                        )
                    it += state
                    messageDispatcher.addListener(state.stateMachine, FsmMessageTypes.ENEMY_IS_HIT.ordinal)

                    val phyCmp = it[PhysicComponent]

                    val pulseLight =
                        lightEngine.addPointLight(
                            phyCmp.body.position + 1f,
                            Color.ORANGE,
                            11f,
                        )
                    pulseLight.effectParams.pulseMaxIntensity = 4f
                    pulseLight.effectParams.pulseMinIntensity = 2f
                    pulseLight.effect = LightEffectType.PULSE
                    pulseLight.b2dLight.attachToBody(phyCmp.body)

                    it += LightComponent(pulseLight)

                    // create normal nearbyEnemiesSensor
                    phyCmp.body.circle(
                        cfg.nearbyEnemiesDefaultSensorRadius,
                        cfg.nearbyEnemiesSensorOffset,
                    ) {
                        isSensor = true
                        userData = FixtureData(SensorType.NEARBY_ENEMY_SENSOR)
                        filter.categoryBits = EntityCategory.SENSOR.bit
                        filter.maskBits = EntityCategory.PLAYER.bit
                    }

                    it += BasicSensorsComponent(chaseRange = cfg.nearbyEnemiesExtendedSensorRadius)

                    it += RayHitComponent()

                    it +=
                        BehaviorTreeComponent(
                            world = world,
                            stage = stage,
                            treePath = cfg.aiTreePath,
                            // The blackboard must be created via a function reference (or lambda)
                            // because at this point we finally have access to the correct Entity, World, and Stage.
                            createBlackboard = { entity, world, stage ->
                                MushroomContext(entity, world, stage, debugRenderService)
                            },
                        )
                }

                else -> return
            }
        }
    }

    /**
     * Calculates and returns the scaled size (`Vector2`) of the first animation frame
     * for the given `AnimationModel`, `AnimationType`, and `AnimationVariant`.
     * Uses a cache to avoid redundant calculations, retrieves the appropriate texture atlas,
     * finds the animation regions, and computes the size based on the original frame dimensions
     * and a unit scale factor. Throws an error if the atlas or regions are missing.
     */
    private fun size(
        model: AnimationModel,
        type: AnimationType,
    ): Vector2 {
        val cacheKey = type.name
        return sizesCache.getOrPut(cacheKey) {
            val atlas =
                atlasMap[model]
                    ?: gdxError("No texture atlas for model '$model' in EntitySpawnSystem found.")

            val regions = atlas.findRegions(type.atlasKey)
            if (regions.isEmpty) gdxError("No regions for the animation '$type' for model '$model' found")

            val firstFrame = regions.first()
            Vector2(firstFrame.originalWidth * UNIT_SCALE, firstFrame.originalHeight * UNIT_SCALE)
        }
    }

    private fun setupShader(name: String): ShaderProgram {
        val vertShader: FileHandle = Gdx.files.internal("shader/$name.vert")
        val fragShader: FileHandle = Gdx.files.internal("shader/$name.frag")
        ShaderProgram.pedantic = false
        val shader = ShaderProgram(vertShader, fragShader)

        if (!shader.isCompiled) {
            throw GdxRuntimeException("Could not compile shader: ${shader.log}")
        }

        shader.bind()
        shader.setUniformi("u_texture", 0)

        return shader
    }

    companion object {
        private val logger = logger<EntitySpawnSystem>()
    }
}
