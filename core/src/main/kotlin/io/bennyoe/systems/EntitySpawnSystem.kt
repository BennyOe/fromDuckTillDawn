package io.bennyoe.systems

import com.badlogic.gdx.ai.msg.MessageManager
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.maps.MapObject
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.Filter
import com.badlogic.gdx.physics.box2d.World
import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Image
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
import io.bennyoe.components.AnimationVariant
import io.bennyoe.components.AttackComponent
import io.bennyoe.components.DeadComponent
import io.bennyoe.components.HealthComponent
import io.bennyoe.components.ImageComponent
import io.bennyoe.components.InputComponent
import io.bennyoe.components.IntentionComponent
import io.bennyoe.components.JumpComponent
import io.bennyoe.components.MoveComponent
import io.bennyoe.components.ShaderRenderingComponent
import io.bennyoe.components.PhysicComponent
import io.bennyoe.components.PlayerComponent
import io.bennyoe.components.SpawnComponent
import io.bennyoe.components.StateComponent
import io.bennyoe.components.ai.BasicSensorsComponent
import io.bennyoe.components.ai.BehaviorTreeComponent
import io.bennyoe.components.ai.NearbyEnemiesComponent
import io.bennyoe.components.ai.RayHitComponent
import io.bennyoe.config.EntityCategory
import io.bennyoe.config.GameConstants.UNIT_SCALE
import io.bennyoe.config.SpawnCfg
import io.bennyoe.event.MapChangedEvent
import io.bennyoe.lightEngine.core.Scene2dLightEngine
import io.bennyoe.service.DefaultDebugRenderService
import io.bennyoe.state.FsmMessageTypes
import io.bennyoe.state.mushroom.MushroomCheckAliveState
import io.bennyoe.state.mushroom.MushroomFSM
import io.bennyoe.state.mushroom.MushroomStateContext
import io.bennyoe.state.player.PlayerCheckAliveState
import io.bennyoe.state.player.PlayerFSM
import io.bennyoe.state.player.PlayerStateContext
import io.bennyoe.utility.BodyData
import io.bennyoe.utility.FixtureData
import io.bennyoe.utility.SensorType
import ktx.app.gdxError
import ktx.box2d.box
import ktx.box2d.circle
import ktx.log.logger
import ktx.math.times
import ktx.math.vec2
import ktx.tiled.layer
import ktx.tiled.type
import ktx.tiled.x
import ktx.tiled.y

class EntitySpawnSystem(
    private val stage: Stage = inject("stage"),
    private val phyWorld: World = inject("phyWorld"),
    private val lightEngine: Scene2dLightEngine = inject("lightEngine"),
    private val debugRenderService: DefaultDebugRenderService = inject("debugRenderService"),
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
                val playerEntityLayer = event.map.layer("playerStart")
                playerEntityLayer.objects.forEach { playerObj ->
                    val cfg = SpawnCfg.createSpawnCfg(playerObj.type!!)
                    createEntity(playerObj, cfg)
                }
                val enemyEntityLayer = event.map.layer("enemies")
                enemyEntityLayer.objects.forEach { enemyObj ->
                    val cfg = SpawnCfg.createSpawnCfg(enemyObj.type!!)
                    createEntity(enemyObj, cfg)
                }

                val lightsLayer = event.map.layer("lights")
                lightsLayer.objects.forEach { light ->
                    val color = light.properties.get("color") as Color
                    val position = vec2(light.x, light.y)
                    createLight(color, position)
                }
                return true
            }
        }
        return false
    }

    private fun createLight(
        color: Color,
        position: Vector2,
    ) {
        lightEngine.setNormalInfluence(.5f)
        val dir = lightEngine.addDirectionalLight(Color(1f, 0f, .5f, .5f), 45f, 1f, 1f)
        dir.b2dLight.isXray = true
        val light =
            lightEngine.addPointLight(
                position * UNIT_SCALE,
                color,
                6f,
                9f,
            )
        light.b2dLight.apply {
            setContactFilter(
                Filter().apply {
                    categoryBits = 0x0008
                    maskBits = EntityCategory.GROUND.bit
                },
            )
        }
        light.b2dLight.isXray = true
    }

    private fun createEntity(
        mapObj: MapObject,
        cfg: SpawnCfg,
    ) {
        val relativeSize = size(cfg.animationModel, cfg.animationType, cfg.animationVariant)
        world.entity {
            // Add general components
            val image =
                // scale sets the image size
                ImageComponent(stage, cfg.scaleImage.x, cfg.scaleImage.y).apply {
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
            animation.nextAnimation(cfg.animationType, cfg.animationVariant)
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
                )
            physics.categoryBits = cfg.entityCategory.bit
            it += physics

            it += HealthComponent()
            it += DeadComponent(cfg.keepCorpse, cfg.removeDelay, cfg.removeDelay)

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

            it += JumpComponent()

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
                        userData = FixtureData(SensorType.GROUND_SENSOR)
                    }

                    val input = InputComponent()
                    it += input

                    it += ShaderRenderingComponent()

                    it += IntentionComponent()

                    val player = PlayerComponent()
                    it += player

                    val state =
                        StateComponent(
                            world = world,
                            owner = PlayerStateContext(entity = it, world = world),
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
                            owner = MushroomStateContext(entity = it, world = world),
                            initialState = MushroomFSM.IDLE,
                            globalState = MushroomCheckAliveState,
                        )
                    it += state
                    messageDispatcher.addListener(state.stateMachine, FsmMessageTypes.ENEMY_IS_HIT.ordinal)

                    val phyCmp = it[PhysicComponent]

                    // create normal nearbyEnemiesSensor
                    val nearbyEnemiesDefaultSensorFixture =
                        phyCmp.body.circle(
                            cfg.nearbyEnemiesDefaultSensorRadius,
                            cfg.nearbyEnemiesSensorOffset,
                        ) {
                            isSensor = true
                            userData = FixtureData(SensorType.NEARBY_ENEMY_SENSOR)
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
        variant: AnimationVariant,
    ): Vector2 {
        val cacheKey = "${type.name}_${variant.name}"
        return sizesCache.getOrPut(cacheKey) {
            val atlas =
                atlasMap[model]
                    ?: gdxError("No texture atlas for model '$model' in EntitySpawnSystem found.")

            val regions = atlas.findRegions(type.atlasKey + variant.atlasKey)
            if (regions.isEmpty) gdxError("No regions for the animation '$type' for model '$model' found")

            val firstFrame = regions.first()
            Vector2(firstFrame.originalWidth * UNIT_SCALE, firstFrame.originalHeight * UNIT_SCALE)
        }
    }

    companion object {
        private val logger = logger<EntitySpawnSystem>()
    }
}
