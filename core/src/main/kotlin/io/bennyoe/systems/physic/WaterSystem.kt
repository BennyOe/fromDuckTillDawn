package io.bennyoe.systems.physic

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.math.GeometryUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.Body
import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.Fixed
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import com.github.quillraven.fleks.World.Companion.inject
import io.bennyoe.components.ParticleComponent
import io.bennyoe.components.ParticleType
import io.bennyoe.components.TransformComponent
import io.bennyoe.components.WaterComponent
import io.bennyoe.config.GameConstants
import io.bennyoe.config.GameConstants.PHYSIC_TIME_STEP
import ktx.math.vec2
import kotlin.math.absoluteValue
import kotlin.math.min
import kotlin.math.sin

private const val SPLASH_WAVES_MULTIPLIER = .2f

const val MIN_SPLASH_AREA: Float = SPLASH_WAVES_MULTIPLIER
const val DRAG_MOD: Float = 0.35f
const val LIFT_MOD: Float = 0.85f
const val MAX_DRAG: Float = 2000f
const val MAX_LIFT: Float = 500f
const val TORQUE_DAMPING = 100f

class WaterSystem(
    private val stage: Stage = inject("stage"),
) : IteratingSystem(
        family { all(WaterComponent) },
        interval = Fixed(PHYSIC_TIME_STEP),
    ) {
    private var spawnedThisTick = false
    private val impulsedBodiesThisTick = hashSetOf<Body>()
    private var continuousTime = 0f

    private val particleCmp =
        ParticleComponent(
            particleFile = Gdx.files.internal("particles/splash.p"),
            scaleFactor = 0.15f,
            motionScaleFactor = 0.2f,
            looping = false,
            stage = stage,
            zIndex = 90000,
            type = ParticleType.WATER_SPLASH,
        )

    override fun onTickEntity(entity: Entity) {
        val waterCmp = entity[WaterComponent]

        val bodiesInContact = waterCmp.fixturePairs.mapTo(hashSetOf()) { it.second.body }

        // Iterate over all current contact pairs between water (fixtureA) and an object (fixtureB)
        for ((fluidFix, objectFix) in waterCmp.fixturePairs) {
            val clipped: MutableList<Vector2> = ArrayList()

            // Compute intersection polygon (object ∩ fluid); skip if there is no overlap
            if (!WaterIntersectionUtils.findIntersectionOfFixtures(fluidFix, objectFix, clipped)) continue

            // --- Buoyancy (Archimedes) ------------------------------------------------------------
            // Build a Polygon from the clipped points to obtain area and centroid
            val intersectionPoly = WaterIntersectionUtils.getIntersectionPolygon(clipped)

            val centroid = Vector2()
            GeometryUtils.polygonCentroid(
                intersectionPoly.vertices,
                0,
                intersectionPoly.vertices.size,
                centroid,
            )
            val area = intersectionPoly.area()
            val displacedMass = waterCmp.density * area

            // Buoyancy force opposes gravity (applied to the object's body at the centroid)
            val objectBody = objectFix.body
            val fluidBody = fluidFix.body

            val gravity = GameConstants.GRAVITY // Vector2(gx, gy)
            val buoyancyForce = Vector2(-0f, -gravity).scl(displacedMass)
            objectBody.applyForce(buoyancyForce, centroid, true)

            // --- Hydrodynamics: drag & lift along each polygon edge -------------------------------
            // We need the closed ring of edges, so we iterate indices and wrap the last-to-first edge.
            val n = clipped.size
            for (i in 0 until n) {
                // Edge endpoints and mid point
                val p1 = clipped[i]
                val p2 = clipped[(i + 1) % n]
                val mid = p1.cpy().add(p2).scl(0.5f)

                // Relative velocity at the mid point (object vs. fluid)
                val relVel =
                    objectBody
                        .getLinearVelocityFromWorldPoint(mid)
                        .sub(fluidBody.getLinearVelocityFromWorldPoint(mid))
                val speed = relVel.len()
                // If speed is ~0, no hydrodynamic forces to apply
                if (speed == 0f) continue
                val vHat = relVel.nor() // unit velocity direction

                // Edge direction and its outward normal (right-handed)
                val edge = p2.cpy().sub(p1)
                val edgeLen = edge.len()
                if (edgeLen == 0f) continue
                val eHat = edge.nor()
                val nHat = Vector2(eHat.y, -eHat.x) // rotate -90°

                // Drag acts opposite to motion and only on leading edges (normal · v ≥ 0)
                val dragDot = nHat.dot(vHat)
                if (dragDot < 0f) continue

                // Common product used by both drag and lift
                val common = edgeLen * waterCmp.density * speed * speed

                // Drag magnitude (clamped)
                val dragMag = min(dragDot * DRAG_MOD * common, MAX_DRAG)
                val dragForce = vHat.cpy().scl(-dragMag)
                objectBody.applyForce(dragForce, mid, true)

                // Lift magnitude (clamped): proportional to alignment of velocity with edge
                val liftDot = eHat.dot(vHat)
                val liftMag = min(dragDot * liftDot * LIFT_MOD * common, MAX_LIFT)
                val liftDir = Vector2(-vHat.y, vHat.x) // v rotated +90°
                val liftForce = liftDir.scl(liftMag)
                objectBody.applyForce(liftForce, mid, true)

                // Gentle angular damping to reduce spinning when interacting with fluid
                objectBody.applyTorque(-objectBody.angularVelocity / TORQUE_DAMPING, true)
            }

            // --- Wave coupling & splashes ---------------------------------------------------------
            if (area > MIN_SPLASH_AREA && clipped.isNotEmpty()) {
                updateColumns(objectBody, clipped, waterCmp)
            }
        }
        waterCmp.enteredBodies.retainAll(bodiesInContact)
        updateWaterSurface(waterCmp)
    }

    private fun updateWaterSurface(waterCmp: WaterComponent) {
        val cols = waterCmp.columns
        val n = cols.size
        if (n <= 1) return

        waterCmp.ensureDeltaCapacity(n)
        val l = waterCmp.lDeltas
        val r = waterCmp.rDeltas

        // --- Part 1: per-column spring update ---
        for (i in 0 until n) {
            val c = cols[i]
            c.update(waterCmp.dampening, waterCmp.tension)
        }

        // --- Part 2: simple traveling sine waves (lightweight) ---
        val t = continuousTime

        // Amplitudes (height), spatial frequencies (k), and angular speeds (w)
        // Keep values small; they add to column speed (velocity)
        val A1 = 0.008f
        val A2 = 0.003f
        val K1 = 6.55f // longer wave (bigger crests)
        val W1 = 7.10f // moves to the right
        val K2 = 2.10f // shorter wave (details)
        val W2 = 6.70f // moves to the left (counter-phase)

        for (i in 0 until n) {
            val c = cols[i]
            val x = c.x
            // Two simple traveling sines; add more if you want richer motion
            val s =
                sin(x * K1 + t * W1) * A1 +
                    sin(x * K2 - t * W2) * A2
            c.speed += s
        }

        // --- Part 3: wave spreading ---
        spreadWaves(waterCmp, n, cols, l, r)
    }

    override fun onTick() {
        spawnedThisTick = false
        impulsedBodiesThisTick.clear()
        continuousTime += deltaTime
        super.onTick()
    }

    private fun updateColumns(
        body: Body,
        intersectionPoints: MutableList<Vector2>,
        waterCmp: WaterComponent,
    ) {
        val minX = intersectionPoints.minOf { it.x }
        val maxX = intersectionPoints.maxOf { it.x }

        waterCmp.columns.forEach { column ->
            if (column.x in minX..maxX) {
                // column points
                val col1 = Vector2(column.x, column.height)
                val col2 = Vector2(column.x, column.y)

                intersectionPoints.zipWithNext().forEach { (p1, p2) ->
                    val intersection = WaterIntersectionUtils.intersection(col1, col2, p1, p2)
                    if (intersection != null && intersection.y < column.height) {
                        val falling = body.linearVelocity.y < 0f
                        if (falling) {
                            // 1) Always excite the column when a falling body crosses it
                            //    Use additive impulse so multiple crossings accumulate naturally
                            column.speed += body.linearVelocity.y * SPLASH_WAVES_MULTIPLIER / 100f

                            // 2) First-ever contact bookkeeping & actualBody
                            val firstContactEver = body !in waterCmp.enteredBodies
                            if (firstContactEver) {
                                waterCmp.enteredBodies += body
                                column.actualBody = body
                            }

                            // 3) At most one particle spawn per tick, regardless of how many columns are excited
                            if ((body !in impulsedBodiesThisTick) && !spawnedThisTick) {
                                spawnedThisTick = true
                                createSplashParticles(column)
                            }
                            impulsedBodiesThisTick += body

                            // 4) For this column, one impulse this tick is enough
                            return@forEach
                        }
                    } else if (body === column.actualBody) {
                        column.actualBody = null
                    }
                }
            }

            val bodyBelow = body.position.y < column.y
            val actualBelow =
                column.actualBody
                    ?.position
                    ?.y
                    ?.let { it < column.y } ?: false
            if (bodyBelow || actualBelow) column.actualBody = null
        }
    }

    private fun createSplashParticles(column: WaterColumn) {
        val body = column.actualBody ?: return

        val bodyVel = body.linearVelocity.y.absoluteValue

        // Ignore very slow movements (no splash effect)
        if (bodyVel <= 3f) return

        world.entity {
            it +=
                TransformComponent(
                    vec2(column.x, column.y + column.height * 0.3f),
                    stage.camera.viewportWidth,
                    stage.camera.viewportHeight,
                )
            it += particleCmp
        }
    }
}
