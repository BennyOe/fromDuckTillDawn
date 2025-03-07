package io.bennyoe.systems

import com.badlogic.gdx.graphics.g2d.TextureAtlas
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
import io.bennyoe.components.AnimationComponent
import io.bennyoe.components.AnimationType
import io.bennyoe.components.ImageComponent
import io.bennyoe.components.MoveComponent
import io.bennyoe.components.PhysicComponent
import io.bennyoe.components.PlayerComponent
import io.bennyoe.components.SpawnCfg
import io.bennyoe.components.SpawnComponent
import io.bennyoe.event.MapChangedEvent
import ktx.app.gdxError
import ktx.log.logger
import ktx.tiled.layer
import ktx.tiled.type
import ktx.tiled.x
import ktx.tiled.y

/*
The spawn system iterates over the entities in a map and spawns them
 */
class EntitySpawnSystem(
    private val stage: Stage = inject(),
    private val phyWorld: World = inject("phyWorld"),
    private val atlas: TextureAtlas = inject()
) :
    IteratingSystem(family { all(SpawnComponent) }), EventListener {
    private val cachedCfgs = mutableMapOf<String, SpawnCfg>()
    private val sizesCache = mutableMapOf<AnimationType, Vector2>()

    override fun onTickEntity(entity: Entity) {
    }

    private fun size(type: AnimationType): Vector2 = sizesCache.getOrPut(type) {
        val regions = atlas.findRegions(type.atlasKey)
        if (regions.isEmpty) gdxError("There are no regions for the idle animation of model $type")
        val firstFrame = regions.first()
        return Vector2(firstFrame.originalWidth * UNIT_SCALE, firstFrame.originalHeight * UNIT_SCALE)
    }

    private fun spawnCfg(type: String): SpawnCfg = cachedCfgs.getOrPut(type) {
        when (type) {
            "player" -> SpawnCfg(AnimationType.IDLE)
            else -> gdxError("There is no spawn configuration for entity-type $type")
        }
    }

    override fun handle(event: Event): Boolean {
        when (event) {
            is MapChangedEvent -> {
                val entityLayer = event.map.layer("entities")
                entityLayer.objects.forEach { mapObj ->
                    val cfg = spawnCfg(mapObj.type!!)
                    val relativeSize = size(cfg.model)
                    world.entity {
                        val animation = AnimationComponent()
                        animation.nextAnimation(AnimationType.IDLE)
                        it += animation

                        val image = ImageComponent(stage, 2f, 1f).apply {
                            image = Image().apply {
                                setPosition(mapObj.x * UNIT_SCALE, mapObj.y * UNIT_SCALE)
                                setSize(relativeSize.x, relativeSize.y)
                            }
                        }
                        it += image

                        val physics = PhysicComponent.physicsComponentFromImage(
                            phyWorld,
                            image.image,
                            BodyDef.BodyType.DynamicBody,
                            scalePhysicX = 0.1f,
                            scalePhysicY = 0.2f
                        )
                        it += physics

                        val move = MoveComponent()
                        it += move

                        val player = PlayerComponent()
                        it += player

                        PlayerInputProcessor(world = world)
                    }
                }
                return true
            }
        }
        return false
    }
    companion object {
        private val LOG = logger<EntitySpawnSystem>()
    }
}
