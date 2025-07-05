package io.bennyoe.systems

import com.badlogic.gdx.maps.tiled.TiledMapTileLayer
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType.StaticBody
import com.badlogic.gdx.physics.box2d.World
import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import com.github.quillraven.fleks.World.Companion.inject
import io.bennyoe.components.PhysicComponent
import io.bennyoe.components.PhysicComponent.Companion.physicsComponentFromShape2D
import io.bennyoe.config.EntityCategory
import io.bennyoe.event.MapChangedEvent
import io.bennyoe.utility.BodyData
import ktx.box2d.body
import ktx.box2d.loop
import ktx.log.logger
import ktx.math.vec2
import ktx.tiled.forEachLayer
import ktx.tiled.height
import ktx.tiled.isEmpty
import ktx.tiled.shape
import ktx.tiled.width
import kotlin.math.max

class CollisionSpawnSystem(
    private val phyWorld: World = inject("phyWorld"),
) : IteratingSystem(family { all(PhysicComponent) }),
    EventListener,
    PausableSystem {
    override fun onTickEntity(entity: Entity) = Unit

    override fun handle(event: Event): Boolean {
        when (event) {
            is MapChangedEvent -> {
                drawTileCollisionBoxes(event)
                drawCollisionBoxes(event)
                drawMapBorderCollisions(event)
            }
        }
        return true
    }

    // draws the auto drawn collision boxes for the tiles
    private fun drawTileCollisionBoxes(event: MapChangedEvent) {
        event.map.forEachLayer<TiledMapTileLayer> { layer ->
            layer.forEachCell(0, 0, max(event.map.width, event.map.height)) { cell, x, y ->

                if (cell.tile.objects.isEmpty()) return@forEachCell

                cell.tile.objects.forEach { mapObject ->
                    world.entity {
                        physicsComponentFromShape2D(
                            phyWorld,
                            mapObject.shape,
                            x,
                            y,
                            setUserData = BodyData(EntityCategory.GROUND, it),
                        )
                    }
                }
            }
        }
    }

    // draws the hand drawn collision boxes
    private fun drawCollisionBoxes(event: MapChangedEvent) {
        event.map.layers.get("collisionBoxes").apply {
            objects.forEach { mapObject ->
                world.entity {
                    physicsComponentFromShape2D(
                        phyWorld = phyWorld,
                        shape = mapObject.shape,
                        setUserData = BodyData(EntityCategory.GROUND, it),
                    )
                }
            }
        }
    }

    private fun drawMapBorderCollisions(event: MapChangedEvent) {
        world.entity {
            val w = event.map.width
            val h = event.map.height
            PhysicComponent().apply {
                body =
                    phyWorld.body(StaticBody) {
                        position.set(0f, 0f)
                        fixedRotation = true
                        allowSleep = false
                        loop(
                            vec2(0f, 0f),
                            vec2(w.toFloat(), 0f),
                            vec2(w.toFloat(), h.toFloat()),
                            vec2(0f, h.toFloat()),
                        ) {
                            friction = 0f
                        }
                        userData = BodyData(EntityCategory.GROUND, it)
                    }
            }
        }
    }

    private fun TiledMapTileLayer.forEachCell(
        startX: Int,
        startY: Int,
        size: Int,
        action: (TiledMapTileLayer.Cell, Int, Int) -> Unit,
    ) {
        for (x in startX - size..startX + size) {
            for (y in startY - size..startY + size) {
                this.getCell(x, y)?.let { action(it, x, y) }
            }
        }
    }

    companion object {
        private val logger = logger<CollisionSpawnSystem>()
    }
}
