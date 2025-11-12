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
import io.bennyoe.systems.debug.DebugType
import io.bennyoe.systems.debug.DefaultDebugRenderService
import io.bennyoe.systems.debug.addToDebugView
import io.bennyoe.utility.EntityBodyData
import ktx.collections.GdxArray
import ktx.log.logger

class BasicSensorsSystem(
    private val phyWorld: World = inject("phyWorld"),
    private val debugRenderingService: DefaultDebugRenderService = inject("debugRenderService"),
) : IteratingSystem(
        family { all(BasicSensorsComponent, RayHitComponent) },
//        interval = Fixed(PHYSIC_TIME_STEP * 3),
    ) {
    private val playerEntity by lazy { world.family { all(PlayerComponent, PhysicComponent) }.first() }

    override fun onTickEntity(entity: Entity) {
        val basicSensorsCmp = entity[BasicSensorsComponent]
        val rayHitCmp = entity[RayHitComponent]
        val phyCmp = entity[PhysicComponent]
        val imageCmp = entity[ImageComponent]
        val intentionCmp = entity[IntentionComponent]
        val bodyPos = phyCmp.body.position
        val bodySize = phyCmp.size
        val flipImg = imageCmp.flipImage
        val playerBodyPos = playerEntity[PhysicComponent].body.position
        val fixtureCenterPos = bodyPos.cpy().add(phyCmp.offset)

        // update sensor positions
        updateSensorPositions(
            basicSensorsCmp,
            fixtureCenterPos,
            flipImg,
            playerBodyPos,
            bodySize,
        )

        clearLedgeHitArrays(rayHitCmp)

        // update upper ledge sensor positions
        createLedgeSensors(
            basicSensorsCmp.upperLedgeSensorArray,
            intentionCmp,
            fixtureCenterPos,
            flipImg,
            rayHitCmp.upperLedgeHits,
            bodySize,
        )
        createLedgeSensors(
            basicSensorsCmp.lowerLedgeSensorArray,
            intentionCmp,
            fixtureCenterPos,
            flipImg,
            rayHitCmp.lowerLedgeHits,
            bodySize,
        )
        rayHitCmp.upperLedgeHits.sort()
        rayHitCmp.lowerLedgeHits.sort()

        val sightSensor = basicSensorsCmp.sightSensor

        if (dst(sightSensor.from.x, sightSensor.from.y, sightSensor.to.x, sightSensor.to.y) < basicSensorsCmp.maxSightRadius) {
            // when sight is not blocked player can be seen. If sight is blocked but still in range player is in throwRange
            processSensor(basicSensorsCmp.sightSensor, phyCmp) { rayHitCmp.seesPlayer = !it }
            rayHitCmp.playerInThrowRange = true
        } else {
            rayHitCmp.seesPlayer = false
            rayHitCmp.playerInThrowRange = false
        }

        processSensor(basicSensorsCmp.wallSensor, phyCmp) { rayHitCmp.wallHit = it }
        processSensor(basicSensorsCmp.wallHeightSensor, phyCmp) { rayHitCmp.wallHeightHit = it }
        processSensor(basicSensorsCmp.groundSensor, phyCmp) { rayHitCmp.groundHit = it }
        processSensor(basicSensorsCmp.jumpSensor, phyCmp) { rayHitCmp.jumpHit = it }
        processSensor(basicSensorsCmp.attackSensor, phyCmp) { rayHitCmp.canAttack = it }
    }

    private fun processSensor(
        sensor: SensorDef,
        phyCmp: PhysicComponent,
        setHit: (Boolean) -> Unit,
    ) {
        sensor.let {
            setHit(false)

            var hitThisTick = false

            phyWorld.rayCast(
                { fixture, point, normal, fraction ->
                    // ignore own body
                    if (fixture.body == phyCmp.body) {
                        return@rayCast -1f
                    }
                    // if sensor has filter for specific type it gets filtered here
                    val bodyData = fixture.body.userData as EntityBodyData?
                    if (sensor.hitFilter != null && bodyData != null && !sensor.hitFilter.invoke(bodyData)) {
                        return@rayCast 1f
                    }

                    hitThisTick = true
                    setHit(true)
                    1f
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
        bodySize: Vector2,
    ) {
        sensorArray.forEach { sensor ->
            if (!intentionCmp.wantsToChase) return@forEach
            sensor.updateAbsolutePositions(bodyPos, flipImg, bodySize)

            var hitGroundThisTick = false

            phyWorld.rayCast(
                { fixture, point, normal, fraction ->
                    val bodyData = fixture.body.userData as EntityBodyData?
                    if (bodyData?.entityCategory == EntityCategory.GROUND) {
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
        bodySize: Vector2,
    ) {
        basicSensorsCmp.wallSensor.updateAbsolutePositions(bodyPos, flipImg, bodySize)
        basicSensorsCmp.wallHeightSensor.updateAbsolutePositions(bodyPos, flipImg, bodySize)
        basicSensorsCmp.groundSensor.updateAbsolutePositions(bodyPos, flipImg, bodySize)
        basicSensorsCmp.jumpSensor.updateAbsolutePositions(bodyPos, flipImg, bodySize)
        basicSensorsCmp.attackSensor.updateAbsolutePositions(bodyPos, flipImg, bodySize)
        basicSensorsCmp.sightSensor.updateSightSensor(bodyPos, playerBodyPos)
    }

    companion object {
        val logger = logger<BasicSensorsSystem>()
    }
}
