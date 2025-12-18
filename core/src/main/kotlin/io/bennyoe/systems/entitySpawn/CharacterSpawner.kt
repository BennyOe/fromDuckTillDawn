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
import io.bennyoe.ai.blackboards.MinotaurContext
import io.bennyoe.ai.blackboards.MushroomContext
import io.bennyoe.assets.TextureAtlases
import io.bennyoe.components.AmbienceZoneContactComponent
import io.bennyoe.components.AttackComponent
import io.bennyoe.components.CharacterTypeComponent
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
import io.bennyoe.components.ai.BasicSensorsHitComponent
import io.bennyoe.components.ai.BehaviorTreeComponent
import io.bennyoe.components.ai.FieldOfViewComponent
import io.bennyoe.components.ai.FieldOfViewResultComponent
import io.bennyoe.components.ai.LedgeSensorsComponent
import io.bennyoe.components.ai.LedgeSensorsHitComponent
import io.bennyoe.components.ai.NearbyEnemiesComponent
import io.bennyoe.components.ai.SuspicionComponent
import io.bennyoe.components.animation.AnimationComponent
import io.bennyoe.components.animation.AnimationKey
import io.bennyoe.components.animation.AnimationModel
import io.bennyoe.components.audio.ReverbZoneContactComponent
import io.bennyoe.components.audio.SoundProfileComponent
import io.bennyoe.config.CharacterType
import io.bennyoe.config.EntityCategory
import io.bennyoe.config.GameConstants.UNIT_SCALE
import io.bennyoe.config.SpawnCfgFactory
import io.bennyoe.lightEngine.core.LightEffectType
import io.bennyoe.lightEngine.core.Scene2dLightEngine
import io.bennyoe.state.FsmMessageTypes
import io.bennyoe.state.minotaur.MinotaurCheckAliveState
import io.bennyoe.state.minotaur.MinotaurFSM
import io.bennyoe.state.minotaur.MinotaurStateContext
import io.bennyoe.state.mushroom.MushroomCheckAliveState
import io.bennyoe.state.mushroom.MushroomFSM
import io.bennyoe.state.mushroom.MushroomStateContext
import io.bennyoe.state.player.PlayerCheckAliveState
import io.bennyoe.state.player.PlayerFSM
import io.bennyoe.state.player.PlayerStateContext
import io.bennyoe.systems.debug.DebugRenderer
import io.bennyoe.systems.render.ZIndex
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
    minotaurAtlases: TextureAtlases,
) {
    private val atlasMap: Map<AnimationModel, TextureAtlas> =
        mapOf(
            AnimationModel.PLAYER_DAWN to dawnAtlases.diffuseAtlas,
            AnimationModel.ENEMY_MUSHROOM to mushroomAtlases.diffuseAtlas,
            AnimationModel.ENEMY_MINOTAUR to minotaurAtlases.diffuseAtlas,
        )

    private val sizesCache = mutableMapOf<String, Vector2>()
    private val messageDispatcher = MessageManager.getInstance()

    fun spawnCharacterObjects(
        characterObjectsLayer: MapLayer,
        layerZIndex: Int,
    ) {
        characterObjectsLayer.objects.forEach { characterObj ->
            val characterType =
                CharacterType.valueOf(characterObj.type?.uppercase() ?: throw IllegalArgumentException("Type must not be null"))
            val cfg = SpawnCfgFactory.createSpawnCfg(characterType)
            val atlasRegionSize = size(cfg.animationModel, cfg.animationType)
            world.entity { entity ->
                // center position directly from tiled (point object)
                val spawnPosCenter = vec2(characterObj.x * UNIT_SCALE, characterObj.y * UNIT_SCALE)

                // calculate visual size
                val visualWidth = atlasRegionSize.x * cfg.scaleImage.x
                val visualHeight = atlasRegionSize.y * cfg.scaleImage.y

                // use visual size for transform
                val transformCmp =
                    TransformComponent(
                        spawnPosCenter,
                        visualWidth,
                        visualHeight,
                    )
                entity += transformCmp

                val image =
                    // scale sets the image size
                    ImageComponent(stage, zIndex = layerZIndex + cfg.zIndex).apply {
                        image =
                            Image().apply {
                                setPosition(0f, 0f)
                            }
                    }
                image.image.name = cfg.entityCategory.name
                entity += image

                val animation = AnimationComponent()
                animation.animationModel = cfg.animationModel
                animation.speedMultiplier = cfg.animationSpeed
                animation.nextAnimation(cfg.animationType)
                animation.animationSoundTriggers = cfg.soundTrigger
                entity += animation

                entity += CharacterTypeComponent(cfg.characterType)

                val physics =
                    PhysicComponent.physicsComponentFromBox(
                        phyWorld,
                        entity,
                        spawnPosCenter,
                        visualWidth,
                        visualHeight,
                        cfg.bodyType,
                        categoryBit = cfg.entityCategory.bit,
                        maskBit = cfg.physicMaskCategory,
                        scalePhysicX = cfg.scalePhysic.x,
                        scalePhysicY = cfg.scalePhysic.y,
                        myFriction = 0f,
                        offsetX = cfg.offsetPhysic.x,
                        offsetY = cfg.offsetPhysic.y,
                        setUserdata = EntityBodyData(entity, cfg.entityCategory),
                        sensorType = SensorType.HITBOX_SENSOR,
                    )
                physics.categoryBits = cfg.entityCategory.bit

                // Ground type sensor
                physics.body.box(
                    physics.size.x * 0.99f,
                    0.01f,
                    Vector2(physics.offset.x, physics.offset.y - physics.size.y * 0.5f),
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
                    Vector2(physics.offset.x, physics.offset.y + physics.size.y * 0.5f),
                ) {
                    isSensor = true
                    userData = FixtureSensorData(entity, SensorType.UNDER_WATER_SENSOR)
                    filter.categoryBits = EntityCategory.SENSOR.bit
                    filter.maskBits = EntityCategory.WATER.bit
                }

                entity += physics

                entity += GroundTypeSensorComponent

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
                    attackCmp.attackMap = cfg.attackMap
                    entity += attackCmp
                }

                entity += ShaderRenderingComponent()

                entity +=
                    JumpComponent(
                        maxHeight = cfg.jumpHeight,
                    )

                entity += SoundProfileComponent(cfg.soundProfile)

                if (cfg.entityCategory == EntityCategory.PLAYER) {
                    spawnPlayerSpecifics(entity, physics)
                }

                if (cfg.entityCategory == EntityCategory.ENEMY) {
                    when (characterType) {
                        CharacterType.MUSHROOM -> spawnMushroomSpecifics(entity, cfg, transformCmp)
                        CharacterType.MINOTAUR -> spawnMinotaurSpecifics(entity, cfg, transformCmp)
                        else -> gdxError("No spawner for character $characterType found")
                    }
                }
            }
        }
    }

    private fun EntityCreateContext.spawnMushroomSpecifics(
        entity: Entity,
        cfg: SpawnCfgFactory,
        transformCmp: TransformComponent,
    ) {
        entity += IntentionComponent()

        entity += NearbyEnemiesComponent()

        val state =
            StateComponent(
                world = world,
                owner = MushroomStateContext(entity, world, stage),
                initialState = MushroomFSM.IDLE(),
                globalState = MushroomCheckAliveState(),
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

        entity +=
            BasicSensorsComponent(
                sensorList = cfg.basicSensorList,
                chaseRange = cfg.nearbyEnemiesExtendedSensorRadius,
                transformCmp = transformCmp,
                maxSightRadius = cfg.maxSightRadius,
            )

        entity += LedgeSensorsComponent()
        entity += LedgeSensorsHitComponent()

        entity += BasicSensorsHitComponent()

        entity += FieldOfViewResultComponent()

        entity +=
            FieldOfViewComponent(
                transformCmp,
                14f,
                relativeEyePos = 0.8f,
                numberOfRays = 9,
                viewAngle = 45f,
            )

        entity += SuspicionComponent()

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

    private fun EntityCreateContext.spawnMinotaurSpecifics(
        entity: Entity,
        cfg: SpawnCfgFactory,
        transformCmp: TransformComponent,
    ) {
        entity += IntentionComponent()

        entity += NearbyEnemiesComponent()

        val state =
            StateComponent(
                world = world,
                owner =
                    MinotaurStateContext(
                        entity = entity,
                        world = world,
                        phyWorld = phyWorld,
                        stage = stage,
                        minotaurAtlas = atlasMap[AnimationModel.ENEMY_MINOTAUR]!!,
                        debugRenderer = debugRenderer,
                    ),
                initialState = MinotaurFSM.IDLE(),
                globalState = MinotaurCheckAliveState(),
            )
        entity += state
        messageDispatcher.addListener(state.stateMachine, FsmMessageTypes.ENEMY_IS_HIT.ordinal)

        val phyCmp = entity[PhysicComponent]

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

        entity +=
            BasicSensorsComponent(
                sensorList = cfg.basicSensorList,
                chaseRange = cfg.nearbyEnemiesExtendedSensorRadius,
                transformCmp = transformCmp,
                maxSightRadius = cfg.maxSightRadius,
                sightSensorDef = cfg.sightSensorDefinition,
            )

        entity += BasicSensorsHitComponent()

        entity +=
            BehaviorTreeComponent(
                world = world,
                stage = stage,
                treePath = cfg.aiTreePath,
                // The blackboard must be created via a function reference (or lambda)
                // because at this point we finally have access to the correct Entity, World, and Stage.
                createBlackboard = { entity, world, stage ->
                    MinotaurContext(entity, world, stage, debugRenderer)
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
            Vector2(physics.offset.x, physics.offset.y - physics.size.y * 0.5f),
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

        entity += AmbienceZoneContactComponent()

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
                zIndex = ZIndex.PARTICLES.value,
                enabled = false,
                type = ParticleType.AIR_BUBBLES,
            )
        entity += particle

        messageDispatcher.addListener(state.stateMachine, FsmMessageTypes.HEAL.ordinal)
        messageDispatcher.addListener(state.stateMachine, FsmMessageTypes.ATTACK.ordinal)
        messageDispatcher.addListener(state.stateMachine, FsmMessageTypes.KILL.ordinal)
        messageDispatcher.addListener(state.stateMachine, FsmMessageTypes.PLAYER_IS_HIT.ordinal)
        messageDispatcher.addListener(state.stateMachine, FsmMessageTypes.PLAYER_IS_GRABBED.ordinal)
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
        type: AnimationKey,
    ): Vector2 {
        val cacheKey = "${model.atlasKey}${type.atlasKey}"
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
}
