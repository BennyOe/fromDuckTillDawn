package io.bennyoe.systems.physic

import com.badlogic.gdx.math.GeometryUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.Body
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import io.bennyoe.components.DRAG_MOD
import io.bennyoe.components.LIFT_MOD
import io.bennyoe.components.MAX_DRAG
import io.bennyoe.components.MAX_LIFT
import io.bennyoe.components.MIN_SPLASH_AREA
import io.bennyoe.components.PhysicComponent
import io.bennyoe.components.TORQUE_DAMPING
import io.bennyoe.components.WaterComponent
import io.bennyoe.config.GameConstants
import kotlin.math.min

class WaterSystem : IteratingSystem(family { all(WaterComponent, PhysicComponent) }) {
    override fun onTickEntity(entity: Entity) {
        val waterCmp = entity[WaterComponent]
        val physicCmp = entity[PhysicComponent]
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
                        if (body.linearVelocity.y < 0 && column.actualBody == null) {
                            column.actualBody = body
                            column.speed = body.linearVelocity.y * 3f / 100f
                            // TODO create splash particles
//                            if (splashParticles) createSplashParticles(column)
                        }
                    }
                }
            } else if (body === column.actualBody) {
                column.actualBody = null
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
}
