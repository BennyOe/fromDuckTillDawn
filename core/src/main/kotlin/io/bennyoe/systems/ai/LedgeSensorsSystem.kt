package io.bennyoe.systems.ai

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Polyline
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.World
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import com.github.quillraven.fleks.World.Companion.inject
import io.bennyoe.components.ImageComponent
import io.bennyoe.components.IntentionComponent
import io.bennyoe.components.PhysicComponent
import io.bennyoe.components.ai.LedgeHitData
import io.bennyoe.components.ai.LedgeSensorsComponent
import io.bennyoe.components.ai.LedgeSensorsHitComponent
import io.bennyoe.components.ai.SensorDef
import io.bennyoe.config.EntityCategory
import io.bennyoe.systems.debug.DebugType
import io.bennyoe.systems.debug.DefaultDebugRenderService
import io.bennyoe.systems.debug.addToDebugView
import io.bennyoe.utility.EntityBodyData
import ktx.collections.GdxArray

class LedgeSensorsSystem(
    private val phyWorld: World = inject("phyWorld"),
    private val debugRenderingService: DefaultDebugRenderService = inject("debugRenderService"),
) : IteratingSystem(
        family { all(LedgeSensorsComponent, LedgeSensorsHitComponent) },
//        interval = Fixed(PHYSIC_TIME_STEP * 3),
    ) {
    override fun onTickEntity(entity: Entity) {
        val ledgeSensorsCmp = entity[LedgeSensorsComponent]
        val intentionCmp = entity[IntentionComponent]
        val ledgeSensorsHitCmp = entity[LedgeSensorsHitComponent]
        val phyCmp = entity[PhysicComponent]
        val imageCmp = entity[ImageComponent]
        val bodyPos = phyCmp.body.position
        val fixtureCenterPos = bodyPos.cpy().add(phyCmp.offset)
        val flipImg = imageCmp.flipImage
        val bodySize = phyCmp.size

        clearLedgeHitArrays(ledgeSensorsHitCmp)

        // update upper ledge sensor positions
        createLedgeSensors(
            ledgeSensorsCmp.upperLedgeSensorArray,
            intentionCmp,
            fixtureCenterPos,
            flipImg,
            ledgeSensorsHitCmp.upperLedgeHits,
            bodySize,
        )
        createLedgeSensors(
            ledgeSensorsCmp.lowerLedgeSensorArray,
            intentionCmp,
            fixtureCenterPos,
            flipImg,
            ledgeSensorsHitCmp.lowerLedgeHits,
            bodySize,
        )
        ledgeSensorsHitCmp.upperLedgeHits.sort()
        ledgeSensorsHitCmp.lowerLedgeHits.sort()
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

    private fun clearLedgeHitArrays(rayHitCmp: LedgeSensorsHitComponent) {
        rayHitCmp.upperLedgeHits.clear()
        rayHitCmp.lowerLedgeHits.clear()
    }
}
