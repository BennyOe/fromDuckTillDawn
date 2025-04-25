package io.bennyoe.systems

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
import io.bennyoe.Duckee.Companion.UNIT_SCALE
import io.bennyoe.PlayerInputProcessor
import io.bennyoe.components.AiComponent
import io.bennyoe.components.AnimationComponent
import io.bennyoe.components.AnimationModel
import io.bennyoe.components.AnimationType
import io.bennyoe.components.AnimationVariant
import io.bennyoe.components.ImageComponent
import io.bennyoe.components.InputComponent
import io.bennyoe.components.MoveComponent
import io.bennyoe.components.PhysicComponent
import io.bennyoe.components.PlayerComponent
import io.bennyoe.components.SpawnCfg
import io.bennyoe.components.SpawnComponent
import io.bennyoe.event.MapChangedEvent
import ktx.app.gdxError
import ktx.box2d.box
import ktx.log.logger
import ktx.tiled.layer
import ktx.tiled.type
import ktx.tiled.x
import ktx.tiled.y

class EntitySpawnSystem(
    private val stage: Stage = inject(),
    private val phyWorld: World = inject("phyWorld"),
    private val atlas: TextureAtlas = inject(),
) : IteratingSystem(family { all(SpawnComponent) }),
    EventListener {
    private val cachedCfgs = mutableMapOf<String, SpawnCfg>()
    private val sizesCache = mutableMapOf<AnimationType, Vector2>()

    override fun onTickEntity(entity: Entity) {
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

    private fun spawnCfg(type: String): SpawnCfg =
        cachedCfgs.getOrPut(type) {
            when (type) {
                "playerStart" -> SpawnCfg(AnimationModel.PLAYER_DAWN, AnimationType.IDLE, AnimationVariant.FIRST)
                "enemy" -> SpawnCfg(AnimationModel.ENEMY_MUSHROOM, AnimationType.IDLE, AnimationVariant.FIRST)
                else -> gdxError("There is no spawn configuration for entity-type $type")
            }
        }

    override fun handle(event: Event): Boolean {
        when (event) {
            is MapChangedEvent -> {
                val playerEntityLayer = event.map.layer("playerStart")
                playerEntityLayer.objects.forEach { mapObj ->
                    val cfg = spawnCfg(mapObj.type!!)
                    createPlayerEntity(mapObj, cfg)
                }
                val enemyEntityLayer = event.map.layer("enemies")
                enemyEntityLayer.objects.forEach { enemyObj ->
                    val cfg = spawnCfg(enemyObj.type!!)
                    createEnemyEntity(enemyObj, cfg)
                }
                return true
            }
        }
        return false
    }

    private fun createEnemyEntity(
        enemyObj: MapObject,
        cfg: SpawnCfg,
    ) {
        val relativeSize = size(cfg.model, cfg.type, cfg.variant)
        world.entity {
            val animation = AnimationComponent()
            animation.nextAnimation(cfg.model, cfg.type, cfg.variant)
            it += animation

            val image =
                ImageComponent(stage, 3f, 3f).apply {
                    image =
                        Image().apply {
                            setPosition(enemyObj.x * UNIT_SCALE, enemyObj.y * UNIT_SCALE)
                            setSize(relativeSize.x, relativeSize.y)
                        }
                }
            it += image

            val physic =
                PhysicComponent.physicsComponentFromImage(
                    phyWorld,
                    image.image,
                    BodyDef.BodyType.DynamicBody,
                    scalePhysicX = 0.2f,
                    scalePhysicY = 0.4f,
                    offsetY = -0.8f,
                    myFriction = 0f,
                )

            it += physic
        }
    }

    private fun createPlayerEntity(
        mapObj: MapObject,
        cfg: SpawnCfg,
    ) {
        val relativeSize = size(cfg.model, cfg.type, cfg.variant)
        world.entity {
            val input = InputComponent()
            it += input

            val animation = AnimationComponent()
            animation.nextAnimation(cfg.model, cfg.type, cfg.variant)
            it += animation

            val image =
                ImageComponent(stage, 4f, 2f).apply {
                    image =
                        Image().apply {
                            setPosition(mapObj.x * UNIT_SCALE, mapObj.y * UNIT_SCALE)
                            setSize(relativeSize.x, relativeSize.y)
                        }
                }
            it += image

            val physics =
                PhysicComponent.physicsComponentFromImage(
                    phyWorld,
                    image.image,
                    BodyDef.BodyType.DynamicBody,
                    scalePhysicX = 0.2f,
                    scalePhysicY = 0.5f,
                )
            // set ground collision sensor
            physics.body.box(physics.size.x * 0.99f, 0.01f, Vector2(0f, 0f - physics.size.y * 0.5f)) {
                isSensor = true
                userData = "GROUND_COLLISION"
            }
            it += physics

            val move = MoveComponent()
            it += move

            val player = PlayerComponent()
            it += player

            val ai = AiComponent(world)
            it += ai

            PlayerInputProcessor(world = world)
        }
    }

    companion object {
        private val logger = logger<EntitySpawnSystem>()
    }
}
