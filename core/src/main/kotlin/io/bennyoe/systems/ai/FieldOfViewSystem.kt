package io.bennyoe.systems.ai

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.MathUtils.acos
import com.badlogic.gdx.math.Polygon
import com.badlogic.gdx.math.Polyline
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.Body
import com.badlogic.gdx.physics.box2d.RayCastCallback
import com.badlogic.gdx.physics.box2d.World
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import com.github.quillraven.fleks.World.Companion.inject
import io.bennyoe.components.ImageComponent
import io.bennyoe.components.PhysicComponent
import io.bennyoe.components.TransformComponent
import io.bennyoe.components.ai.FieldOfViewComponent
import io.bennyoe.components.ai.FieldOfViewResultComponent
import io.bennyoe.components.characterMarker.PlayerComponent
import io.bennyoe.config.EntityCategory
import io.bennyoe.config.GameConstants.ENABLE_DEBUG
import io.bennyoe.systems.debug.DebugType
import io.bennyoe.systems.debug.DefaultDebugRenderService
import io.bennyoe.systems.debug.addToDebugView
import io.bennyoe.utility.bodyData
import ktx.log.logger
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sign
import kotlin.math.sqrt

private const val PLAYER_HEIGHT_OFFSET = 0.8f

class FieldOfViewSystem(
    private val phyWorld: World = inject("phyWorld"),
    private val debugRenderingService: DefaultDebugRenderService = inject("debugRenderService"),
) : IteratingSystem(family { all(FieldOfViewComponent) }) {
    private val playerEntity by lazy { world.family { all(PlayerComponent, PhysicComponent) }.first() }
    private val fovPoly = Polygon()

    private var numberOfRaysHitting: Int = 0

    // Pre-allocate vectors to prevent Garbage Collection issues during game loop
    private val rayStart = Vector2()
    private val rayEnd = Vector2()
    private val lookingDirVec = Vector2()
    private var playerPosX = 0f
    private var playerPosYTop = 0f
    private var playerPosYBott = 0f

    // State variables for the callback
    private var hadRelevantHit = false
    private var closestFraction = 1f
    private var closestHitIsPlayer = false
    private var currentOwnerBody: Body? = null

    // Reusable Callback to avoid creating a new lambda object for every single ray
    private val rayCastCallback =
        RayCastCallback { fixture, _, _, fraction ->
            if (fixture.body == currentOwnerBody) return@RayCastCallback -1f

            val bodyData = fixture.bodyData

            val isPlayer = bodyData?.entity == playerEntity
            val isGround = bodyData?.entityCategory == EntityCategory.GROUND

            if (!isPlayer && !isGround) {
                return@RayCastCallback -1f
            }

            hadRelevantHit = true
            if (fraction < closestFraction) {
                closestFraction = fraction
                closestHitIsPlayer = isPlayer
            }

            return@RayCastCallback closestFraction
        }

    override fun onTickEntity(entity: Entity) {
        val playerTransformCmp = playerEntity[TransformComponent]
        val phyCmp = entity[PhysicComponent]
        val fieldOfViewCmp = entity[FieldOfViewComponent]
        val fieldOfViewResultCmp = entity[FieldOfViewResultComponent]

        fieldOfViewResultCmp.raysHitting = 0

        val lookingDir = if (entity[ImageComponent].flipImage) lookingDirVec.set(-1f, 0f) else lookingDirVec.set(1f, 0f)

        val enemyEyePosX = phyCmp.body.position.x
        val enemyEyePosY = phyCmp.body.position.y - fieldOfViewCmp.relativeEyePos

        playerPosX = playerTransformCmp.position.x
        playerPosYTop = playerTransformCmp.position.y - playerTransformCmp.height * 0.5f
        playerPosYBott = playerTransformCmp.position.y + playerTransformCmp.height * 0.5f

        val dx = playerPosX - enemyEyePosX
        val dyTop = playerPosYTop - enemyEyePosY
        val dyBott = playerPosYBott - enemyEyePosY

        // 1) vertical gate (is player too high or too low)
        if (abs(dyBott) > fieldOfViewCmp.maxVerticalDistance) {
            return
        }

        // 2) range check
        val distanceToPlayer = dx * dx + min(dyTop, dyBott) * min(dyTop, dyBott)

        if (distanceToPlayer > fieldOfViewCmp.maxDistance) {
            return
        }

        // 3) check for close range (2.1 meters) and do the raycast if player is close. This ensures that the FOV check is skipped for too short
        // distances and ignores the player due to a too steep viewing angle.
        val closeRangeSq = 2.1f * 2.1f
        if (distanceToPlayer <= closeRangeSq && dx.sign == lookingDir.x.sign) {
            if (processRaycast(playerTransformCmp, fieldOfViewCmp, phyCmp)) {
                fieldOfViewResultCmp.distanceToPlayer = distanceToPlayer
                fieldOfViewResultCmp.raysHitting = numberOfRaysHitting
                return
            }
        }

        // 3) FOV angle check
        val lenTop = sqrt(dx * dx + dyTop * dyTop)
        val lenBott = sqrt(dx * dx + dyBott * dyBott)

        // safeguard for len == 0
        if (lenTop < 0.0001f || lenBott < 0.0001f) return

        val dxNor = dx / lenTop
        val dyNorTop = dyTop / lenTop
        val dyNorBott = dyBott / lenBott

        val dotProductTop = lookingDir.x * dxNor + lookingDir.y * dyNorTop
        val dotProductBott = lookingDir.x * dxNor + lookingDir.y * dyNorBott

        if (dotProductTop < fieldOfViewCmp.fovThreshold && dotProductBott < fieldOfViewCmp.fovThreshold) {
            return
        }

        drawDebugFieldOfView(fieldOfViewCmp, enemyEyePosX, enemyEyePosY, lookingDir, entity)

        if (processRaycast(playerTransformCmp, fieldOfViewCmp, phyCmp)) {
            fieldOfViewResultCmp.distanceToPlayer = distanceToPlayer
            fieldOfViewResultCmp.raysHitting = numberOfRaysHitting
        }
    }

    private fun drawDebugFieldOfView(
        fieldOfViewCmp: FieldOfViewComponent,
        eyeX: Float,
        eyeY: Float,
        lookingDir: Vector2,
        entity: Entity,
    ) {
        if (!ENABLE_DEBUG) return
        val halfAngleRad = acos(fieldOfViewCmp.fovThreshold)
        val distance = sqrt(fieldOfViewCmp.maxDistance)

        val segments = 10
        val vertices = FloatArray(2 + (segments + 1) * 2)

        vertices[0] = eyeX
        vertices[1] = eyeY

        val baseAngleRad = lookingDir.angleRad()
        val startAngleRad = baseAngleRad - halfAngleRad
        val stepAngle = (halfAngleRad * 2) / segments

        for (i in 0..segments) {
            val currentAngle = startAngleRad + stepAngle * i

            val px = eyeX + MathUtils.cos(currentAngle) * distance
            val py = eyeY + MathUtils.sin(currentAngle) * distance

            val offset = 2 + i * 2
            vertices[offset] = px
            vertices[offset + 1] = py
        }

        fovPoly.vertices = vertices

        fovPoly.addToDebugView(
            debugRenderingService,
            Color.GREEN,
            debugType = DebugType.ENEMY,
            type = ShapeRenderer.ShapeType.Line,
            alpha = 0.4f,
            label = "fov_triangle_${entity.id}",
        )
    }

    private fun processRaycast(
        playerTransformCmp: TransformComponent,
        fieldOfViewCmp: FieldOfViewComponent,
        phyCmp: PhysicComponent,
    ): Boolean {
        rayStart.set(phyCmp.body.position).sub(0f, fieldOfViewCmp.relativeEyePos)

        // Set context for the callback
        currentOwnerBody = phyCmp.body
        numberOfRaysHitting = 0

        val targetHeight = playerTransformCmp.height * PLAYER_HEIGHT_OFFSET
        val playerTop = playerTransformCmp.position.y + targetHeight * 0.5f
        val playerBottom = playerTransformCmp.position.y - targetHeight * 0.5f

        val rayCount = fieldOfViewCmp.numberOfRays.coerceAtLeast(3)
        val step = (playerTop - playerBottom) / (rayCount - 1)

        repeat(rayCount) { index ->
            val targetY = playerBottom + step * index

            rayEnd.set(playerTransformCmp.position.x, targetY)

            hadRelevantHit = false
            closestFraction = 1f
            closestHitIsPlayer = false

            phyWorld.rayCast(
                rayCastCallback,
                rayStart,
                rayEnd,
            )

            val rayCountsAsHit = hadRelevantHit && closestHitIsPlayer
            if (rayCountsAsHit) numberOfRaysHitting++

            val rayColor =
                when {
                    rayCountsAsHit -> Color.GREEN
                    else -> Color.RED
                }

            if (ENABLE_DEBUG) {
                Polyline(floatArrayOf(rayStart.x, rayStart.y, rayEnd.x, rayEnd.y)).addToDebugView(
                    debugRenderingService,
                    rayColor,
                    debugType = DebugType.ENEMY,
                )
            }
        }
        return numberOfRaysHitting > 0
    }

    companion object {
        val logger = logger<FieldOfViewSystem>()
    }
}
