package io.bennyoe.systems

import com.badlogic.gdx.ai.fsm.DefaultStateMachine
import com.badlogic.gdx.ai.msg.MessageManager
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.maps.MapObject
import com.badlogic.gdx.math.Vector2
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
import io.bennyoe.components.AnimationComponent
import io.bennyoe.components.AnimationModel
import io.bennyoe.components.AnimationType
import io.bennyoe.components.AnimationVariant
import io.bennyoe.components.AttackComponent
import io.bennyoe.components.HealthComponent
import io.bennyoe.components.ImageComponent
import io.bennyoe.components.InputComponent
import io.bennyoe.components.IntentionComponent
import io.bennyoe.components.JumpComponent
import io.bennyoe.components.MoveComponent
import io.bennyoe.components.PhysicComponent
import io.bennyoe.components.PlayerComponent
import io.bennyoe.components.SpawnComponent
import io.bennyoe.components.StateComponent
import io.bennyoe.components.ai.BehaviorTreeComponent
import io.bennyoe.components.ai.NearbyEnemiesComponent
import io.bennyoe.config.EntityCategory
import io.bennyoe.config.GameConstants.UNIT_SCALE
import io.bennyoe.config.SpawnCfg
import io.bennyoe.event.MapChangedEvent
import io.bennyoe.state.FsmMessageTypes
import io.bennyoe.state.player.PlayerCheckAliveState
import io.bennyoe.state.player.PlayerFSM
import io.bennyoe.state.player.PlayerStateContext
import io.bennyoe.utility.BodyData
import io.bennyoe.utility.FixtureData
import io.bennyoe.utility.FixtureType
import ktx.app.gdxError
import ktx.box2d.box
import ktx.box2d.circle
import ktx.log.logger
import ktx.tiled.layer
import ktx.tiled.type
import ktx.tiled.x
import ktx.tiled.y

class EntitySpawnSystem(
    private val stage: Stage = inject("stage"),
    private val phyWorld: World = inject("phyWorld"),
    private val atlas: TextureAtlas = inject(),
) : IteratingSystem(family { all(SpawnComponent) }),
    EventListener {
    private val sizesCache = mutableMapOf<AnimationType, Vector2>()
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
                return true
            }
        }
        return false
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
                    scalePhysicX = cfg.scalePhysic.x,
                    scalePhysicY = cfg.scalePhysic.y,
                    myFriction = 0f,
                    offsetY = cfg.offsetPhysic.y,
                    setUserdata = BodyData(cfg.entityCategory, it),
                )
            physics.categoryBits = cfg.entityCategory.bit
            it += physics

            it += HealthComponent()

            val move = MoveComponent()
            move.maxSpeed *= cfg.scaleSpeed
            it += move

            if (cfg.canAttack) {
                val attackCmp = AttackComponent()
                attackCmp.extraRange *= cfg.attackExtraRange
                attackCmp.maxDamage *= cfg.scaleAttackDamage
                attackCmp.attackDelay = cfg.attackDelay
                it += attackCmp
            }

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
                        userData = FixtureData(FixtureType.GROUND_SENSOR)
                    }

                    val input = InputComponent()
                    it += input

                    it += IntentionComponent()

                    it += JumpComponent()

                    val player = PlayerComponent()
                    it += player

                    val state =
                        StateComponent(
                            world = world,
                            owner = PlayerStateContext(entity = it, world = world),
                            initialState = PlayerFSM.IDLE,
                            globalState = PlayerCheckAliveState,
                            factory = ::DefaultStateMachine,
                        )
                    it += state

                    messageDispatcher.addListener(state.stateMachine, FsmMessageTypes.HEAL.ordinal)
                    messageDispatcher.addListener(state.stateMachine, FsmMessageTypes.ATTACK.ordinal)
                    messageDispatcher.addListener(state.stateMachine, FsmMessageTypes.KILL.ordinal)
                    messageDispatcher.addListener(state.stateMachine, FsmMessageTypes.HIT.ordinal)

                    PlayerInputProcessor(world = world)
                }

                // Add Enemy specific components
                EntityCategory.ENEMY -> {
                    val phyCmp = it[PhysicComponent]
                    // this is the mushroom entity
                    phyCmp.body.circle(
                        4f,
                        Vector2(0f, 0f),
                    ) {
                        isSensor = true
                        userData = FixtureData(FixtureType.NEARBY_ENEMY_SENSOR)
                    }

                    it += IntentionComponent()

                    it += NearbyEnemiesComponent()

                    it +=
                        BehaviorTreeComponent(
                            world = world,
                            stage = stage,
                            treePath = cfg.aiTreePath,
                            // The blackboard must be created via a function reference (or lambda)
                            // because at this point we finally have access to the correct Entity, World, and Stage.
                            createBlackboard = ::MushroomContext,
                        )
                }

                else -> return
            }
        }
    }

    private fun size(
        model: AnimationModel,
        type: AnimationType,
        variant: AnimationVariant,
    ): Vector2 =
        sizesCache.getOrPut(type) {
            val regions = atlas.findRegions(model.atlasKey + type.atlasKey + variant.atlasKey)
            if (regions.isEmpty) gdxError("There are no regions for the idle animation of model $type")
            val firstFrame = regions.first()
            return Vector2(firstFrame.originalWidth * UNIT_SCALE, firstFrame.originalHeight * UNIT_SCALE)
        }

    companion object {
        private val logger = logger<EntitySpawnSystem>()
    }
}
