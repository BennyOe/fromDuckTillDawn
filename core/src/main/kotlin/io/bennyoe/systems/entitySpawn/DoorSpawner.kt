package io.bennyoe.systems.entitySpawn

import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.maps.MapLayer
import com.badlogic.gdx.physics.box2d.BodyDef
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import io.bennyoe.components.DoorComponent
import io.bennyoe.components.DoorTriggerComponent
import io.bennyoe.components.ImageComponent
import io.bennyoe.components.PhysicComponent
import io.bennyoe.components.TransformComponent
import io.bennyoe.config.EntityCategory
import io.bennyoe.config.GameConstants.UNIT_SCALE
import io.bennyoe.lightEngine.core.Scene2dLightEngine
import io.bennyoe.utility.EntityBodyData
import io.bennyoe.utility.SensorType
import ktx.log.logger
import ktx.math.vec2
import ktx.tiled.height
import ktx.tiled.shape
import ktx.tiled.width
import ktx.tiled.x
import ktx.tiled.y
import kotlin.experimental.or

class DoorSpawner(
    val world: World,
    val stage: Stage,
    val phyWorld: com.badlogic.gdx.physics.box2d.World,
    val lightEngine: Scene2dLightEngine,
    val worldObjectsAtlas: TextureAtlas,
) {
    private val doorTriggerMap = mutableMapOf<String, Entity>()

    fun spawnDoors(
        doorObjectsLayer: MapLayer,
        layerZIndex: Int,
    ) {
        doorObjectsLayer.objects.forEach { doorObj ->
            val id = doorObj.properties.get("id") as String
            val textureName = doorObj.properties.get("texture") as? String ?: ""
            val positionBottomLeft = vec2(doorObj.x * UNIT_SCALE, doorObj.y * UNIT_SCALE)
            val width = doorObj.width * UNIT_SCALE
            val height = doorObj.height * UNIT_SCALE
            val positionCenter =
                vec2(
                    positionBottomLeft.x + width * 0.5f,
                    positionBottomLeft.y + height * 0.5f,
                )

            world.entity {
                doorTriggerMap[id] = it
                it += DoorComponent(id)

                val imageCmp = ImageComponent(stage, zIndex = layerZIndex)
                imageCmp.image = Image(worldObjectsAtlas.findRegion(textureName))
                imageCmp.image.setSize(width, height)
                imageCmp.image.setPosition(positionBottomLeft.x, positionBottomLeft.y)

                it += imageCmp

                it += TransformComponent(positionCenter, width, height)

                it +=
                    PhysicComponent.physicsComponentFromBox(
                        phyWorld = phyWorld,
                        entity = it,
                        positionCenter,
                        width,
                        height,
                        bodyType = BodyDef.BodyType.StaticBody,
                        categoryBit = EntityCategory.GROUND.bit,
                        maskBit = EntityCategory.PLAYER.bit or EntityCategory.ENEMY.bit,
                        setUserdata = EntityBodyData(it, EntityCategory.GROUND),
                    )
            }
        }
    }

    fun spawnTrigger(triggerObjectsLayer: MapLayer) {
        triggerObjectsLayer.objects.forEach { triggerObj ->
            val targetId = triggerObj.properties.get("targetId") as String
            world.entity {
                it += DoorTriggerComponent(doorTriggerMap[targetId]!!)
                it +=
                    PhysicComponent.physicsComponentFromShape2D(
                        phyWorld = phyWorld,
                        entity = it,
                        shape = triggerObj.shape,
                        isSensor = true,
                        categoryBit = EntityCategory.SENSOR.bit,
                        maskBit = EntityCategory.PLAYER.bit,
                        sensorType = SensorType.DOOR_TRIGGER_SENSOR,
                        setUserData = EntityBodyData(it, EntityCategory.SENSOR),
                    )
            }
        }
    }

    companion object {
        val logger = logger<DoorSpawner>()
    }
}
