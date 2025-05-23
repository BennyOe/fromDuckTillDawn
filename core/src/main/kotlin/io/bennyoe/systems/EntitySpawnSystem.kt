package io.bennyoe.systems

import com.badlogic.gdx.ai.msg.MessageManager
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.maps.MapObject
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.BodyDef
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
import io.bennyoe.components.AnimationComponent
import io.bennyoe.components.AnimationModel
import io.bennyoe.components.AnimationType
import io.bennyoe.components.AnimationVariant
import io.bennyoe.components.AttackComponent
import io.bennyoe.components.HealthComponent
import io.bennyoe.components.ImageComponent
import io.bennyoe.components.InputComponent
import io.bennyoe.components.JumpComponent
import io.bennyoe.components.MoveComponent
import io.bennyoe.components.PhysicComponent
import io.bennyoe.components.PlayerComponent
import io.bennyoe.components.SpawnComponent
import io.bennyoe.components.StateComponent
import io.bennyoe.config.EntityCategory
import io.bennyoe.config.GameConstants.UNIT_SCALE
import io.bennyoe.config.SpawnCfg
import io.bennyoe.event.MapChangedEvent
import io.bennyoe.state.FsmMessageTypes
import ktx.app.gdxError
import ktx.box2d.box
import ktx.log.logger
import ktx.math.vec2
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
    private val cachedSpawnCfgs = mutableMapOf<String, SpawnCfg>()
    private val sizesCache = mutableMapOf<AnimationType, Vector2>()
    private val messageDispatcher = MessageManager.getInstance()

    override fun onTickEntity(entity: Entity) {
    }

    override fun handle(event: Event): Boolean {
        when (event) {
            is MapChangedEvent -> {
                val playerEntityLayer = event.map.layer("playerStart")
                playerEntityLayer.objects.forEach { playerObj ->
                    val cfg = createSpawnCfg(playerObj.type!!)
                    createEntity(playerObj, cfg)
                }
                val enemyEntityLayer = event.map.layer("enemies")
                enemyEntityLayer.objects.forEach { enemyObj ->
                    val cfg = createSpawnCfg(enemyObj.type!!)
                    createEntity(enemyObj, cfg)
                }
                return true
            }
        }
        return false
    }

    private fun createSpawnCfg(type: String): SpawnCfg =
        cachedSpawnCfgs.getOrPut(type) {
            when (type) {
                "playerStart" ->
                    SpawnCfg(
                        animationModel = AnimationModel.PLAYER_DAWN,
                        animationType = AnimationType.IDLE,
                        animationVariant = AnimationVariant.FIRST,
                        bodyType = BodyDef.BodyType.DynamicBody,
                        entityCategory = EntityCategory.PLAYER.bit,
                        canAttack = true,
                        scaleImage = vec2(4f, 2f),
                        scalePhysic = vec2(0.2f, 0.5f),
                    )

                "enemy" ->
                    SpawnCfg(
                        animationModel = AnimationModel.ENEMY_MUSHROOM,
                        animationType = AnimationType.IDLE,
                        animationVariant = AnimationVariant.FIRST,
                        bodyType = BodyDef.BodyType.DynamicBody,
                        entityCategory = EntityCategory.ENEMY.bit,
                        canAttack = true,
                        scaleImage = vec2(3f, 3f),
                        scalePhysic = vec2(0.2f, 0.4f),
                        offsetPhysic = vec2(0f, -0.8f),
                    )

                else -> gdxError("There is no spawn configuration for entity-type $type")
            }
        }

    private fun createEntity(
        mapObj: MapObject,
        cfg: SpawnCfg,
    ) {
        val relativeSize = size(cfg.animationModel, cfg.animationType, cfg.animationVariant)
        world.entity {
            val input = InputComponent()
            it += input

            val animation = AnimationComponent()
            animation.nextAnimation(cfg.animationModel, cfg.animationType, cfg.animationVariant)
            it += animation

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

            val physics =
                PhysicComponent.physicsComponentFromImage(
                    phyWorld,
                    image.image,
                    cfg.bodyType,
                    categoryBit = cfg.entityCategory,
                    scalePhysicX = cfg.scalePhysic.x,
                    scalePhysicY = cfg.scalePhysic.y,
                    offsetY = cfg.offsetPhysic.y,
                    setUserdata = it,
                )
            // set ground collision sensor
            physics.body.box(
                physics.size.x * 0.99f,
                0.01f,
                Vector2(0f, 0f - physics.size.y * 0.5f),
            ) {
                isSensor = true
                userData = "GROUND_COLLISION"
            }
            physics.categoryBits = cfg.entityCategory
            it += physics

            it += HealthComponent()

            if (cfg.canAttack) {
                val attackCmp = AttackComponent()
                attackCmp.extraRange *= cfg.attackExtraRange
                attackCmp.maxDamage *= cfg.scaleAttackDamage
                it += attackCmp
            }

            // Player specific
            if (cfg.animationModel == AnimationModel.PLAYER_DAWN) {
                it += JumpComponent()

                val move = MoveComponent()
                it += move

                val player = PlayerComponent()
                it += player

                val state = StateComponent(world)
                messageDispatcher.addListener(state.stateMachine, FsmMessageTypes.HEAL.ordinal)
                messageDispatcher.addListener(state.stateMachine, FsmMessageTypes.ATTACK.ordinal)
                messageDispatcher.addListener(state.stateMachine, FsmMessageTypes.KILL.ordinal)
                it += state

                PlayerInputProcessor(world = world)
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
