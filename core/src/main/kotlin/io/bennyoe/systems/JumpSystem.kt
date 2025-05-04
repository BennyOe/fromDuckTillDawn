package io.bennyoe.systems

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.World
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import com.github.quillraven.fleks.World.Companion.inject
import io.bennyoe.GameConstants.PHYSIC_TIME_STEP
import io.bennyoe.ai.PlayerFSM
import io.bennyoe.components.AiComponent
import io.bennyoe.components.HasGroundContact
import io.bennyoe.components.JumpComponent
import kotlin.math.sqrt

class JumpSystem(
    val physicWorld: World = inject("phyWorld"),
) : IteratingSystem(family { all(JumpComponent) }) {
    private val gravityPerStep = Vector2()

    // formula taken from: https://www.iforce2d.net/b2dtut/projected-trajectory
    private fun getJumpVelocity(desiredHeight: Float): Float {
        if (desiredHeight <= 0) {
            return 0f
        }

        // do calculation in physic step time unit
        gravityPerStep.set(physicWorld.gravity).scl(PHYSIC_TIME_STEP).scl(PHYSIC_TIME_STEP)

        val a = 0.5f / gravityPerStep.y
        val b = 0.5f

        val quadraticSolution1 = (-b - sqrt(b * b - 4 * a * desiredHeight)) / (2 * a)
        val quadraticSolution2 = (-b + sqrt(b * b - 4 * a * desiredHeight)) / (2 * a)

        // convert result back to "per second"
        return if (quadraticSolution1 < 0) {
            quadraticSolution2 / PHYSIC_TIME_STEP
        } else {
            quadraticSolution1 / PHYSIC_TIME_STEP
        }
    }

    override fun onTickEntity(entity: Entity) {
        val jumpCmp = entity[JumpComponent]
        val aiCmp = entity[AiComponent]
        jumpCmp.jumpVelocity = getJumpVelocity(jumpCmp.maxHeight)

        if (aiCmp.stateMachine.currentState == PlayerFSM.FALL) {
            jumpCmp.doubleJumpGraceTimer -= deltaTime
        }

        if (entity has HasGroundContact) {
            jumpCmp.resetDoubleJumpGraceTimer()
        }
    }

    companion object {
        val logger = ktx.log.logger<JumpSystem>()
    }
}
