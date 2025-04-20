package io.bennyoe.systems

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.World
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import io.bennyoe.Duckee
import io.bennyoe.components.JumpComponent
import kotlin.math.sqrt

class JumpSystem(val physicWorld: World) : IteratingSystem(family { all(JumpComponent) }) {
    private val gravityPerStep = Vector2()

    // formula taken from: https://www.iforce2d.net/b2dtut/projected-trajectory
    private fun getJumpVelocity(desiredHeight: Float): Float {
        if (desiredHeight <= 0) {
            return 0f
        }

        // do calculation in physic step time unit
        gravityPerStep.set(physicWorld.gravity).scl(Duckee.PHYSIC_TIME_STEP).scl(Duckee.PHYSIC_TIME_STEP)

        val a = 0.5f / gravityPerStep.y
        val b = 0.5f

        val quadraticSolution1 = (-b - sqrt(b * b - 4 * a * desiredHeight)) / (2 * a)
        val quadraticSolution2 = (-b + sqrt(b * b - 4 * a * desiredHeight)) / (2 * a)

        // convert result back to "per second"
        return if (quadraticSolution1 < 0) {
            quadraticSolution2 / Duckee.PHYSIC_TIME_STEP
        } else {
            quadraticSolution1 / Duckee.PHYSIC_TIME_STEP
        }
    }

    override fun onTickEntity(entity: Entity) {
        val jumpComponent = entity[JumpComponent]
        jumpComponent.jumpVelocity = getJumpVelocity(jumpComponent.maxHeight)
    }

    companion object {
        val logger = ktx.log.logger<JumpSystem>()
    }
}
