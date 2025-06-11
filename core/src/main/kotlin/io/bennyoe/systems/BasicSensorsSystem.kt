package io.bennyoe.systems

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Polyline
import com.badlogic.gdx.physics.box2d.World
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.Fixed
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import com.github.quillraven.fleks.World.Companion.inject
import io.bennyoe.components.ImageComponent
import io.bennyoe.components.PhysicComponent
import io.bennyoe.components.ai.BasicSensorsComponent
import io.bennyoe.components.ai.RayHitComponent
import io.bennyoe.config.EntityCategory
import io.bennyoe.config.GameConstants.PHYSIC_TIME_STEP
import io.bennyoe.service.DefaultDebugRenderService
import io.bennyoe.service.addToDebugView
import io.bennyoe.systems.debug.DebugType
import io.bennyoe.utility.BodyData
import ktx.log.logger
import ktx.math.plus
import ktx.math.vec2

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

        basicSensorsCmp.wallSensor.let {
            val rayStart = phyCmp.body.position + it.locationOffset
            val rayEnd =
                if (imageCmp.flipImage) {
                    vec2(rayStart.x - it.length.x, rayStart.y + it.length.y)
                } else {
                    vec2(rayStart.x + it.length.x, rayStart.y + it.length.y)
                }

            rayHitCmp.wallHit = false
            phyWorld.rayCast(
                { fixture, point, normal, fraction ->
                    val bodyData = fixture.body.userData as BodyData
                    if (bodyData.type != EntityCategory.PLAYER) {
                        rayHitCmp.wallHit = true
                    }
                    0f
                },
                rayStart,
                rayEnd,
            )
            Polyline(floatArrayOf(rayStart.x, rayStart.y, rayEnd.x, rayEnd.y)).addToDebugView(
                debugRenderingService,
                Color.BLUE,
                debugType = DebugType.PLAYER,
                label = "wall sensor",
            )
        }

        basicSensorsCmp.groundSensor.let {
            val locationOffsetX = if (imageCmp.flipImage) -it.locationOffset.x else it.locationOffset.x
            val rayStart = phyCmp.body.position + vec2(locationOffsetX, it.locationOffset.y)
            val rayEnd = vec2(rayStart.x + it.length.x, rayStart.y + it.length.y)
            rayHitCmp.groundHit = false
            phyWorld.rayCast(
                { fixture, point, normal, fraction ->
                    rayHitCmp.groundHit = true
                    0f
                },
                rayStart,
                rayEnd,
            )
            Polyline(floatArrayOf(rayStart.x, rayStart.y, rayEnd.x, rayEnd.y)).addToDebugView(
                debugRenderingService,
                Color.ORANGE,
                debugType = DebugType.PLAYER,
                label = "ground sensor",
            )
        }

        basicSensorsCmp.jumpSensor.let {
            val locationOffsetX = if (imageCmp.flipImage) -it.locationOffset.x else it.locationOffset.x
            val rayStart = phyCmp.body.position + vec2(locationOffsetX, it.locationOffset.y)
            val rayEnd = vec2(rayStart.x + it.length.x, rayStart.y + it.length.y)
            rayHitCmp.jumpHit = false
            phyWorld.rayCast(
                { fixture, point, normal, fraction ->
                    rayHitCmp.jumpHit = true
                    0f
                },
                rayStart,
                rayEnd,
            )
            Polyline(floatArrayOf(rayStart.x, rayStart.y, rayEnd.x, rayEnd.y)).addToDebugView(
                debugRenderingService,
                Color.MAGENTA,
                debugType = DebugType.PLAYER,
                label = "jump sensor",
            )
        }
    }

    private fun spawnRays(playerEntity: Entity) {
        val phyCmp = playerEntity[PhysicComponent.Companion]
        val imageCmp = playerEntity[ImageComponent.Companion]
        for (range in -20..20 step 4) {
            val rangeInFloat = range.toFloat() / 10
            val rayLength = 3
            val rayStart = phyCmp.body.position
            val rayEnd =
                if (imageCmp.flipImage) {
                    vec2(rayStart.x - rayLength, rayStart.y - rangeInFloat)
                } else {
                    vec2(rayStart.x + rayLength, rayStart.y + rangeInFloat)
                }

            phyWorld.rayCast({ fixture, point, normal, fraction ->
                logger.debug { "Hit fixture ${fixture.body.userData}" }
                1f
            }, rayStart, rayEnd)
            Polyline(floatArrayOf(rayStart.x, rayStart.y, rayEnd.x, rayEnd.y)).addToDebugView(
                debugRenderingService,
                Color.CHARTREUSE,
                debugType =
                    DebugType.PLAYER,
            )
        }
    }

    companion object {
        val logger = logger<BasicSensorsSystem>()
    }
}
