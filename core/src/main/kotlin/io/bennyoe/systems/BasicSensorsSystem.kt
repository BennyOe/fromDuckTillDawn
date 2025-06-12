package io.bennyoe.systems

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Polyline
import com.badlogic.gdx.physics.box2d.World
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import com.github.quillraven.fleks.World.Companion.inject
import io.bennyoe.components.ImageComponent
import io.bennyoe.components.PhysicComponent
import io.bennyoe.components.ai.BasicSensorsComponent
import io.bennyoe.components.ai.LedgeHitData
import io.bennyoe.components.ai.RayHitComponent
import io.bennyoe.config.EntityCategory
import io.bennyoe.service.DefaultDebugRenderService
import io.bennyoe.service.addToDebugView
import io.bennyoe.systems.debug.DebugType
import io.bennyoe.utility.BodyData
import ktx.log.logger
import ktx.math.plus

class BasicSensorsSystem(
    private val phyWorld: World = inject("phyWorld"),
    private val debugRenderingService: DefaultDebugRenderService = inject("debugRenderService"),
) : IteratingSystem(
        family { all(BasicSensorsComponent, RayHitComponent) },
//        interval = Fixed(PHYSIC_TIME_STEP * 3),
    ) {
    override fun onTickEntity(entity: Entity) {
        val basicSensorsCmp = entity[BasicSensorsComponent]
        val rayHitCmp = entity[RayHitComponent]
        val phyCmp = entity[PhysicComponent]
        val imageCmp = entity[ImageComponent]
        val bodyPos = phyCmp.body.position
        val flipImg = imageCmp.flipImage

        // update sensor positions
        basicSensorsCmp.wallSensor.updateAbsolute(bodyPos, flipImg)
        basicSensorsCmp.groundSensor.updateAbsolute(bodyPos, flipImg)
        basicSensorsCmp.jumpSensor.updateAbsolute(bodyPos, flipImg)
        basicSensorsCmp.wallHeightSensor.updateAbsolute(bodyPos, flipImg)

        rayHitCmp.upperLedgeHits.clear()
        rayHitCmp.lowerLedgeHits.clear()
        // update upper ledge sensor positions
        basicSensorsCmp.upperLedgeSensorArray.forEach { sensor ->
            sensor.updateAbsolute(bodyPos, flipImg)
            phyWorld.rayCast(
                { fixture, point, normal, fraction ->
                    val bodyData = fixture.body.userData as BodyData
                    if (bodyData.type == EntityCategory.GROUND) {
                        rayHitCmp.upperLedgeHits.add(LedgeHitData(true, sensor.from.x))
                    } else {
                        rayHitCmp.upperLedgeHits.add(LedgeHitData(false, sensor.from.x))
                    }
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
                Color.BLUE,
                debugType = DebugType.ENEMY,
            )
        }

        // update lower ledge sensor positions
        basicSensorsCmp.lowerLedgeSensorArray.forEach { sensor ->
            sensor.updateAbsolute(bodyPos, flipImg)
            phyWorld.rayCast(
                { fixture, point, normal, fraction ->
                    val bodyData = fixture.body.userData as BodyData
                    if (bodyData.type == EntityCategory.GROUND) {
                        rayHitCmp.lowerLedgeHits.add(LedgeHitData(true, sensor.from.x))
                    } else {
                        rayHitCmp.lowerLedgeHits.add(LedgeHitData(false, sensor.from.x))
                    }
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
                Color.GREEN,
                debugType = DebugType.ENEMY,
            )
        }

        basicSensorsCmp.wallSensor.let {
            rayHitCmp.wallHit = false
            rayHitCmp.canAttack = false
            phyWorld.rayCast(
                { fixture, point, normal, fraction ->
                    val bodyData = fixture.body.userData as BodyData
                    if (bodyData.type != EntityCategory.PLAYER) {
                        rayHitCmp.wallHit = true
                    }
                    if (bodyData.type == EntityCategory.PLAYER) {
                        rayHitCmp.canAttack = true
                    }
                    0f
                },
                basicSensorsCmp.wallSensor.from,
                basicSensorsCmp.wallSensor.to,
            )
            Polyline(
                floatArrayOf(
                    basicSensorsCmp.wallSensor.from.x,
                    basicSensorsCmp.wallSensor.from.y,
                    basicSensorsCmp.wallSensor.to.x,
                    basicSensorsCmp.wallSensor.to.y,
                ),
            ).addToDebugView(
                debugRenderingService,
                Color.BLUE,
                debugType = DebugType.ENEMY,
                label = "wall sensor",
            )
        }

        basicSensorsCmp.wallHeightSensor.let {
            rayHitCmp.wallHeightHit = false
            phyWorld.rayCast(
                { fixture, point, normal, fraction ->
                    val bodyData = fixture.body.userData as BodyData
                    if (bodyData.type != EntityCategory.PLAYER) {
                        rayHitCmp.wallHit = true
                    }
                    0f
                },
                basicSensorsCmp.wallHeightSensor.from,
                basicSensorsCmp.wallHeightSensor.to,
            )
            Polyline(
                floatArrayOf(
                    basicSensorsCmp.wallHeightSensor.from.x,
                    basicSensorsCmp.wallHeightSensor.to.y,
                    basicSensorsCmp.wallHeightSensor.from.x,
                    basicSensorsCmp.wallHeightSensor.to.y,
                ),
            ).addToDebugView(
                debugRenderingService,
                Color.BLUE,
                debugType = DebugType.ENEMY,
                label = "wall height sensor",
            )
        }

        basicSensorsCmp.groundSensor.let {
            rayHitCmp.groundHit = false
            phyWorld.rayCast(
                { fixture, point, normal, fraction ->
                    rayHitCmp.groundHit = true
                    0f
                },
                basicSensorsCmp.groundSensor.from,
                basicSensorsCmp.groundSensor.to,
            )
            Polyline(
                floatArrayOf(
                    basicSensorsCmp.groundSensor.from.x,
                    basicSensorsCmp.groundSensor.from.y,
                    basicSensorsCmp.groundSensor.to.x,
                    basicSensorsCmp.groundSensor.to.y,
                ),
            ).addToDebugView(
                debugRenderingService,
                Color.ORANGE,
                debugType = DebugType.ENEMY,
                label = "ground sensor",
            )
        }

        basicSensorsCmp.jumpSensor.let {
            rayHitCmp.jumpHit = false
            phyWorld.rayCast(
                { fixture, point, normal, fraction ->
                    rayHitCmp.jumpHit = true
                    0f
                },
                basicSensorsCmp.jumpSensor.from,
                basicSensorsCmp.jumpSensor.to,
            )
            Polyline(
                floatArrayOf(
                    basicSensorsCmp.jumpSensor.from.x,
                    basicSensorsCmp.jumpSensor.from.y,
                    basicSensorsCmp.jumpSensor.to.x,
                    basicSensorsCmp.jumpSensor.to.y,
                ),
            ).addToDebugView(
                debugRenderingService,
                Color.MAGENTA,
                debugType = DebugType.ENEMY,
                label = "jump sensor",
            )
        }
    }

    companion object {
        val logger = logger<BasicSensorsSystem>()
    }
}
