package io.bennyoe.systems

import com.badlogic.gdx.math.Polyline
import com.badlogic.gdx.math.Vector2.dst
import com.badlogic.gdx.physics.box2d.World
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import com.github.quillraven.fleks.World.Companion.inject
import io.bennyoe.components.ImageComponent
import io.bennyoe.components.PhysicComponent
import io.bennyoe.components.PlayerComponent
import io.bennyoe.components.ai.BasicSensorsComponent
import io.bennyoe.components.ai.BasicSensorsHitComponent
import io.bennyoe.components.ai.SensorDef
import io.bennyoe.systems.debug.DebugType
import io.bennyoe.systems.debug.DefaultDebugRenderService
import io.bennyoe.systems.debug.addToDebugView
import io.bennyoe.utility.EntityBodyData
import io.bennyoe.utility.SensorType
import ktx.log.logger

class BasicSensorsSystem(
    private val phyWorld: World = inject("phyWorld"),
    private val debugRenderingService: DefaultDebugRenderService = inject("debugRenderService"),
) : IteratingSystem(
        family { all(BasicSensorsComponent, BasicSensorsHitComponent) },
//        interval = Fixed(PHYSIC_TIME_STEP * 3),
    ) {
    private val playerEntity by lazy { world.family { all(PlayerComponent, PhysicComponent) }.first() }

    override fun onTickEntity(entity: Entity) {
        val basicSensorsCmp = entity[BasicSensorsComponent]
        val rayHitCmp = entity[BasicSensorsHitComponent]
        val phyCmp = entity[PhysicComponent]
        val imageCmp = entity[ImageComponent]
        val bodyPos = phyCmp.body.position
        val flipImg = imageCmp.flipImage
        val fixtureCenterPos = bodyPos.cpy().add(phyCmp.offset)

        basicSensorsCmp.sightSensorDef?.let { sightSensor ->
            val playerBodyPos = playerEntity[PhysicComponent].body.position
            sightSensor.updateSightSensor(fixtureCenterPos, playerBodyPos)

            if (dst(sightSensor.from.x, sightSensor.from.y, sightSensor.to.x, sightSensor.to.y) < basicSensorsCmp.maxSightRadius) {
                // when sight is not blocked player can be seen. If sight is blocked but still in range player is in throwRange
                processSensor(sightSensor, phyCmp) { rayHitCmp.seesPlayer = !it }
                rayHitCmp.playerInThrowRange = true
            } else {
                rayHitCmp.playerInThrowRange = false
                rayHitCmp.seesPlayer = false
            }
        }

        basicSensorsCmp.sensorList.forEach { sensor ->
            sensor.updateAbsolutePositions(
                fixtureCenterPos,
                flipImg,
                phyCmp.size,
            )

            processSensor(sensor, phyCmp) { isHit ->
                when (sensor.type) {
                    SensorType.WALL_SENSOR -> rayHitCmp.wallHit = isHit
                    SensorType.WALL_HEIGHT_SENSOR -> rayHitCmp.wallHeightHit = isHit
                    SensorType.GROUND_DETECT_SENSOR -> rayHitCmp.groundHit = isHit
                    SensorType.JUMP_SENSOR -> rayHitCmp.jumpHit = isHit
                    SensorType.ATTACK_SENSOR -> rayHitCmp.canAttack = isHit
                    else -> Unit
                }
            }
        }
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

    companion object {
        val logger = logger<BasicSensorsSystem>()
    }
}
