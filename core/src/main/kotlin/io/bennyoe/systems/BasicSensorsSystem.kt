package io.bennyoe.systems

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Polyline
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector2.dst
import com.badlogic.gdx.physics.box2d.World
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import com.github.quillraven.fleks.World.Companion.inject
import io.bennyoe.components.ImageComponent
import io.bennyoe.components.IntentionComponent
import io.bennyoe.components.PhysicComponent
import io.bennyoe.components.PlayerComponent
import io.bennyoe.components.ai.BasicSensorsComponent
import io.bennyoe.components.ai.LedgeHitData
import io.bennyoe.components.ai.RayHitComponent
import io.bennyoe.components.ai.SensorDef
import io.bennyoe.config.EntityCategory
import io.bennyoe.config.GameConstants.CHASE_DETECTION_RADIUS
import io.bennyoe.service.DefaultDebugRenderService
import io.bennyoe.service.addToDebugView
import io.bennyoe.systems.debug.DebugType
import io.bennyoe.utility.BodyData
import ktx.collections.GdxArray
import ktx.log.logger

class BasicSensorsSystem(
    private val phyWorld: World = inject("phyWorld"),
    private val debugRenderingService: DefaultDebugRenderService = inject("debugRenderService"),
) : IteratingSystem(
        family { all(BasicSensorsComponent, RayHitComponent) },
//        interval = Fixed(PHYSIC_TIME_STEP * 3),
    ) {
    override fun onTickEntity(entity: Entity) {
        val playerEntity = world.family { all(PlayerComponent, PhysicComponent) }.first()
        val basicSensorsCmp = entity[BasicSensorsComponent]
        val rayHitCmp = entity[RayHitComponent]
        val phyCmp = entity[PhysicComponent]
        val imageCmp = entity[ImageComponent]
        val intentionCmp = entity[IntentionComponent]
        val bodyPos = phyCmp.body.position
        val flipImg = imageCmp.flipImage
        val playerBodyPos = playerEntity[PhysicComponent].body.position

        // update sensor positions
        updateSensorPositions(basicSensorsCmp, bodyPos, flipImg, playerBodyPos)

        clearLedgeHitArrays(rayHitCmp)

        // update upper ledge sensor positions
        createLedgeSensors(basicSensorsCmp.upperLedgeSensorArray, intentionCmp, bodyPos, flipImg, rayHitCmp.upperLedgeHits)
        createLedgeSensors(basicSensorsCmp.lowerLedgeSensorArray, intentionCmp, bodyPos, flipImg, rayHitCmp.lowerLedgeHits)

        val sightSensor = basicSensorsCmp.sightSensor
        if (dst(sightSensor.from.x, sightSensor.from.y, sightSensor.to.x, sightSensor.to.y) < CHASE_DETECTION_RADIUS) {
            processSensor(basicSensorsCmp.sightSensor) { rayHitCmp.sightIsBlocked = it }
        }
        processSensor(basicSensorsCmp.wallSensor) { rayHitCmp.wallHit = it }
        processSensor(basicSensorsCmp.wallHeightSensor) { rayHitCmp.wallHeightHit = it }
        processSensor(basicSensorsCmp.groundSensor) { rayHitCmp.groundHit = it }
        processSensor(basicSensorsCmp.jumpSensor) { rayHitCmp.jumpHit = it }
        processSensor(basicSensorsCmp.attackSensor) { rayHitCmp.canAttack = it }
    }

    private fun processSensor(
        sensor: SensorDef,
        setHit: (Boolean) -> Unit,
    ) {
        sensor.let {
            setHit(false)

            var hitThisTick = false

            phyWorld.rayCast(
                { fixture, point, normal, fraction ->
                    hitThisTick = true
                    setHit(true)
                    0f
                },
                sensor.from,
                sensor.to,
            )
            Polyline(
                floatArrayOf(
                    sensor.from.x,
                    sensor.from.y,
                    sensor.to.x,
                    sensor.to.y,
                ),
            ).addToDebugView(
                debugRenderingService,
                if (hitThisTick) sensor.highlightColor else sensor.color,
                debugType = DebugType.ENEMY,
                label = sensor.name,
            )
        }
    }

    private fun createLedgeSensors(
        sensorArray: GdxArray<SensorDef>,
        intentionCmp: IntentionComponent,
        bodyPos: Vector2,
        flipImg: Boolean,
        rayHitArray: GdxArray<LedgeHitData>,
    ) {
        sensorArray.forEach { sensor ->
            if (!intentionCmp.wantsToChase) return@forEach
            sensor.updateAbsolutePositions(bodyPos, flipImg)

            var hitGroundThisTick = false

            phyWorld.rayCast(
                { fixture, point, normal, fraction ->
                    val bodyData = fixture.body.userData as BodyData
                    if (bodyData.type == EntityCategory.GROUND) {
                        rayHitArray.add(LedgeHitData(true, sensor.from.x))
                        hitGroundThisTick = true
                        return@rayCast 1f
                    }
                    1f
                },
                sensor.from,
                sensor.to,
            )

            if (!hitGroundThisTick) {
                rayHitArray.add(LedgeHitData(false, sensor.from.x))
            }

            Polyline(
                floatArrayOf(
                    sensor.from.x,
                    sensor.from.y,
                    sensor.to.x,
                    sensor.to.y,
                ),
            ).addToDebugView(
                debugRenderingService,
                if (hitGroundThisTick) Color.RED else Color.BLUE,
                debugType = DebugType.ENEMY,
            )
        }
    }

    private fun clearLedgeHitArrays(rayHitCmp: RayHitComponent) {
        rayHitCmp.upperLedgeHits.clear()
        rayHitCmp.lowerLedgeHits.clear()
    }

    private fun updateSensorPositions(
        basicSensorsCmp: BasicSensorsComponent,
        bodyPos: Vector2,
        flipImg: Boolean,
        playerBodyPos: Vector2,
    ) {
        basicSensorsCmp.wallSensor.updateAbsolutePositions(bodyPos, flipImg)
        basicSensorsCmp.wallHeightSensor.updateAbsolutePositions(bodyPos, flipImg)
        basicSensorsCmp.groundSensor.updateAbsolutePositions(bodyPos, flipImg)
        basicSensorsCmp.jumpSensor.updateAbsolutePositions(bodyPos, flipImg)
        basicSensorsCmp.attackSensor.updateAbsolutePositions(bodyPos, flipImg)
        basicSensorsCmp.sightSensor.updateSightSensor(bodyPos, playerBodyPos)
    }

    companion object {
        val logger = logger<BasicSensorsSystem>()
    }
}
