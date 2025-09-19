package io.bennyoe.systems.entitySpawn

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ai.msg.MessageManager
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.maps.MapLayer
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.EntityCreateContext
import com.github.quillraven.fleks.World
import io.bennyoe.PlayerInputProcessor
import io.bennyoe.ai.blackboards.MushroomContext
import io.bennyoe.assets.TextureAtlases
import io.bennyoe.components.AnimationComponent
import io.bennyoe.components.AnimationModel
import io.bennyoe.components.AnimationType
import io.bennyoe.components.AttackComponent
import io.bennyoe.components.DeadComponent
import io.bennyoe.components.FlashlightComponent
import io.bennyoe.components.GroundTypeSensorComponent
import io.bennyoe.components.HealthComponent
import io.bennyoe.components.ImageComponent
import io.bennyoe.components.InputComponent
import io.bennyoe.components.IntentionComponent
import io.bennyoe.components.JumpComponent
import io.bennyoe.components.LightComponent
import io.bennyoe.components.MoveComponent
import io.bennyoe.components.ParticleComponent
import io.bennyoe.components.ParticleType
import io.bennyoe.components.PhysicComponent
import io.bennyoe.components.PlayerComponent
import io.bennyoe.components.ShaderRenderingComponent
import io.bennyoe.components.StateComponent
import io.bennyoe.components.TransformComponent
import io.bennyoe.components.ai.BasicSensorsComponent
import io.bennyoe.components.ai.BehaviorTreeComponent
import io.bennyoe.components.ai.NearbyEnemiesComponent
import io.bennyoe.components.ai.RayHitComponent
import io.bennyoe.components.audio.ReverbZoneContactComponent
import io.bennyoe.components.audio.SoundProfileComponent
import io.bennyoe.config.EntityCategory
import io.bennyoe.config.GameConstants
import io.bennyoe.config.SpawnCfg
import io.bennyoe.lightEngine.core.LightEffectType
import io.bennyoe.lightEngine.core.Scene2dLightEngine
import io.bennyoe.state.FsmMessageTypes
import io.bennyoe.state.mushroom.MushroomCheckAliveState
import io.bennyoe.state.mushroom.MushroomFSM
import io.bennyoe.state.mushroom.MushroomStateContext
import io.bennyoe.state.player.PlayerCheckAliveState
import io.bennyoe.state.player.PlayerFSM
import io.bennyoe.state.player.PlayerStateContext
import io.bennyoe.systems.debug.DebugRenderer
import io.bennyoe.utility.EntityBodyData
import io.bennyoe.utility.FixtureSensorData
import io.bennyoe.utility.SensorType
import ktx.app.gdxError
import ktx.box2d.box
import ktx.box2d.circle
import ktx.math.plus
import ktx.math.vec2
import ktx.tiled.type
import ktx.tiled.x
import ktx.tiled.y

class CharacterSpawner(
    val world: World,
    val phyWorld: com.badlogic.gdx.physics.box2d.World,
    val lightEngine: Scene2dLightEngine,
    val stage: Stage,
    val debugRenderer: DebugRenderer,
    dawnAtlases: TextureAtlases,
    mushroomAtlases: TextureAtlases,
) {
    private val atlasMap: Map<AnimationModel, TextureAtlas> =
        mapOf(
            AnimationModel.PLAYER_DAWN to dawnAtlases.diffuseAtlas,
            AnimationModel.ENEMY_MUSHROOM to mushroomAtlases.diffuseAtlas,
        )

    private val sizesCache = mutableMapOf<String, Vector2>()
    private val messageDispatcher = MessageManager.getInstance()

    fun spawnCharacterObjects(
        characterObjectsLayer: MapLayer,
        layerZIndex: Int,
    ) {
        characterObjectsLayer.objects.forEach { characterObj ->
            val cfg = SpawnCfg.createSpawnCfg(characterObj.type ?: throw IllegalArgumentException("Type must not be null"))
            val relativeSize = size(cfg.animationModel, cfg.animationType)
            world.entity { entity ->
                // Add general components
                val image =
                    // scale sets the image size
                    ImageComponent(stage, cfg.scaleImage.x, cfg.scaleImage.y, zIndex = layerZIndex + cfg.zIndex).apply {
                        image =
                            Image().apply {
                                setPosition(characterObj.x * GameConstants.UNIT_SCALE, characterObj.y * GameConstants.UNIT_SCALE)
                                // this size sets the physic box
                                setSize(relativeSize.x, relativeSize.y)
                            }
                    }
                image.image.name = cfg.entityCategory.name
                entity += image

                val animation = AnimationComponent()
                animation.animationModel = cfg.animationModel
                animation.nextAnimation(cfg.animationType)
                animation.animationSoundTriggers = cfg.soundTrigger
                entity += animation

                val physics =
                    PhysicComponent.physicsComponentFromImage(
                        phyWorld,
                        entity,
                        image.image,
                        cfg.bodyType,
                        categoryBit = cfg.entityCategory.bit,
                        maskBit = cfg.physicMaskCategory,
                        scalePhysicX = cfg.scalePhysic.x,
                        scalePhysicY = cfg.scalePhysic.y,
                        myFriction = 0f,
                        offsetY = cfg.offsetPhysic.y,
                        setUserdata = EntityBodyData(entity, cfg.entityCategory),
                        sensorType = SensorType.HITBOX_SENSOR,
                    )
                physics.categoryBits = cfg.entityCategory.bit

                // Ground type sensor
                physics.body.box(
                    physics.size.x * 0.99f,
                    0.01f,
                    Vector2(0f, 0f - physics.size.y * 0.5f) + cfg.offsetPhysic.y,
                ) {
                    isSensor = true
                    userData = FixtureSensorData(entity, SensorType.GROUND_TYPE_SENSOR)
                    filter.categoryBits = EntityCategory.SENSOR.bit
                    filter.maskBits = EntityCategory.GROUND.bit
                }

                // Underwater sensor
                physics.body.box(
                    physics.size.x * 0.99f,
                    0.01f,
                    Vector2(0f, 0f + physics.size.y * 0.5f) + cfg.offsetPhysic.y,
                ) {
                    isSensor = true
                    userData = FixtureSensorData(entity, SensorType.UNDER_WATER_SENSOR)
                    filter.categoryBits = EntityCategory.SENSOR.bit
                    filter.maskBits = EntityCategory.WATER.bit
                }

                entity += physics

                entity += GroundTypeSensorComponent

                entity +=
                    TransformComponent(
                        vec2(physics.body.position.x, physics.body.position.y),
                        physics.size.x,
                        physics.size.y,
                    )
                entity += HealthComponent()
                entity +=
                    DeadComponent(
                        cfg.keepCorpse,
                        cfg.removeDelay,
                        cfg.removeDelay,
                    )

                val move = MoveComponent()
                move.maxWalkSpeed *= cfg.scaleSpeed
                move.chaseSpeed = cfg.chaseSpeed
                entity += move

                if (cfg.canAttack) {
                    val attackCmp = AttackComponent()
                    attackCmp.extraRange *= cfg.attackExtraRange
                    attackCmp.maxDamage *= cfg.scaleAttackDamage
                    attackCmp.attackDelay = cfg.attackDelay
                    entity += attackCmp
                }

                entity += ShaderRenderingComponent()

                entity += JumpComponent()

                entity += SoundProfileComponent(cfg.soundProfile)

                when (cfg.entityCategory) {
                    EntityCategory.PLAYER -> spawnPlayerSpecifics(entity, physics)

                    EntityCategory.ENEMY -> spawnEnemySpecifics(entity, cfg)

                    else -> throw IllegalArgumentException("Unsupported character type for 'EntityCategory': ${cfg.entityCategory}")
                }
            }
        }
    }

    private fun EntityCreateContext.spawnEnemySpecifics(
        entity: Entity,
        cfg: SpawnCfg,
    ) {
        entity += IntentionComponent()

        entity += NearbyEnemiesComponent()

        val state =
            StateComponent(
                world = world,
                owner = MushroomStateContext(entity, world, stage),
                initialState = MushroomFSM.IDLE,
                globalState = MushroomCheckAliveState,
            )
        entity += state
        messageDispatcher.addListener(state.stateMachine, FsmMessageTypes.ENEMY_IS_HIT.ordinal)

        val phyCmp = entity[PhysicComponent]

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

        entity += LightComponent(pulseLight)

        // create normal nearbyEnemiesSensor
        phyCmp.body.circle(
            cfg.nearbyEnemiesDefaultSensorRadius,
            cfg.nearbyEnemiesSensorOffset,
        ) {
            isSensor = true
            userData = FixtureSensorData(entity, SensorType.NEARBY_ENEMY_SENSOR)
            filter.categoryBits = EntityCategory.SENSOR.bit
            filter.maskBits = EntityCategory.PLAYER.bit
        }

        entity += BasicSensorsComponent(chaseRange = cfg.nearbyEnemiesExtendedSensorRadius)

        entity += RayHitComponent()

        entity +=
            BehaviorTreeComponent(
                world = world,
                stage = stage,
                treePath = cfg.aiTreePath,
                // The blackboard must be created via a function reference (or lambda)
                // because at this point we finally have access to the correct Entity, World, and Stage.
                createBlackboard = { entity, world, stage ->
                    MushroomContext(entity, world, stage, debugRenderer)
                },
            )
    }

    private fun EntityCreateContext.spawnPlayerSpecifics(
        entity: Entity,
        physics: PhysicComponent,
    ) {
        val phyCmp = entity[PhysicComponent]
        phyCmp.body.box(
            physics.size.x * 0.99f,
            0.01f,
            Vector2(0f, 0f - physics.size.y * 0.5f),
        ) {
            isSensor = true
            userData = FixtureSensorData(entity, SensorType.GROUND_DETECT_SENSOR)
            filter.categoryBits = EntityCategory.SENSOR.bit
            filter.maskBits = EntityCategory.GROUND.bit
        }
        val flashlightSpot =
            lightEngine
                .addSpotLight(
                    position = phyCmp.body.position,
                    color = Color.WHITE,
                    direction = 0f,
                    coneDegree = 30f,
                    initialIntensity = 1.8f,
                    b2dDistance = 12f,
                    falloffProfile = 0.4f,
                    shaderIntensityMultiplier = 0.9f,
                ).apply {
                    setOn(false)
                }

        val flashLightHalo =
            lightEngine
                .addPointLight(
                    position = phyCmp.body.position,
                    color = Color.WHITE,
                    initialIntensity = 1f,
                    b2dDistance = 1f,
                    falloffProfile = 0.4f,
                    shaderIntensityMultiplier = 1f,
                ).apply {
                    setOn(false)
                }

        entity += FlashlightComponent(flashlightSpot, flashLightHalo)

        entity += ReverbZoneContactComponent()

        val input = InputComponent()
        entity += input

        entity += IntentionComponent()

        val player = PlayerComponent()
        entity += player

        val state =
            StateComponent(
                world = world,
                owner = PlayerStateContext(entity, world, stage),
                initialState = PlayerFSM.IDLE,
                globalState = PlayerCheckAliveState,
            )
        entity += state

        // spawn air bubbles
        val particle =
            ParticleComponent(
                particleFile = Gdx.files.internal("particles/air.p"),
                scaleFactor = .2f,
                motionScaleFactor = .05f,
                looping = true,
                stage = stage,
                zIndex = 60000,
                enabled = false,
                type = ParticleType.AIR_BUBBLES,
            )
        entity += particle

        messageDispatcher.addListener(state.stateMachine, FsmMessageTypes.HEAL.ordinal)
        messageDispatcher.addListener(state.stateMachine, FsmMessageTypes.ATTACK.ordinal)
        messageDispatcher.addListener(state.stateMachine, FsmMessageTypes.KILL.ordinal)
        messageDispatcher.addListener(state.stateMachine, FsmMessageTypes.PLAYER_IS_HIT.ordinal)

        PlayerInputProcessor(world = world)
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
            Vector2(firstFrame.originalWidth * GameConstants.UNIT_SCALE, firstFrame.originalHeight * GameConstants.UNIT_SCALE)
        }
    }
}
