package io.bennyoe.systems

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.World
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import com.github.quillraven.fleks.World.Companion.inject
import io.bennyoe.components.HasGroundContact
import io.bennyoe.components.InputComponent
import io.bennyoe.components.IntentionComponent
import io.bennyoe.components.JumpComponent
import io.bennyoe.components.MoveComponent
import io.bennyoe.components.PhysicComponent
import io.bennyoe.components.StateComponent
import io.bennyoe.config.GameConstants.FALL_GRAVITY_SCALE
import io.bennyoe.config.GameConstants.JUMP_CUT_FACTOR
import io.bennyoe.config.GameConstants.PHYSIC_TIME_STEP
import io.bennyoe.state.player.PlayerFSM
import kotlin.math.sqrt

class JumpSystem(
    val physicWorld: World = inject("phyWorld"),
) : IteratingSystem(family { all(JumpComponent) }),
    PausableSystem {
    override fun onTickEntity(entity: Entity) {
        val jumpCmp = entity[JumpComponent]
        val stateCmp = entity[StateComponent]
        val physicCmp = entity[PhysicComponent]
        val inputCmp = entity.getOrNull(InputComponent)
        val intentionCmp = entity[IntentionComponent]
        val moveCmp = entity[MoveComponent]
        jumpCmp.jumpVelocity = getJumpVelocity(jumpCmp.maxHeight)

        val vel = physicCmp.body.linearVelocity

        if (moveCmp.lockMovement) {
            return
        }

        // if jumpKey is released and still jumping -> cut the jump velocity
        if (inputCmp != null) {
            handleJumpKeyReleasedWhileJumping(inputCmp, vel, jumpCmp)
        }

        calculateJumpBuffer(intentionCmp, entity, jumpCmp)

        // get extra fall speed
        physicCmp.body.gravityScale = if (vel.y < 0f) FALL_GRAVITY_SCALE else 1f

        when (stateCmp.stateMachine.currentState) {
            PlayerFSM.FALL -> jumpCmp.doubleJumpGraceTimer -= deltaTime
            PlayerFSM.DOUBLE_JUMP -> jumpCmp.disableDoubleJumpGraceTimer()
            else -> Unit
        }

        if (entity has HasGroundContact) {
            if (jumpCmp.jumpBuffer > 0f) {
                logger.debug { "Jump from BUFFER " }
                jumpCmp.jumpFromBuffer = true
            }
            jumpCmp.disableJumpBuffer()
            jumpCmp.resetDoubleJumpGraceTimer()
        }
    }

    private fun handleJumpKeyReleasedWhileJumping(
        inputCmp: InputComponent,
        vel: Vector2,
        jumpCmp: JumpComponent,
    ) {
        if (!inputCmp.jumpIsPressed && vel.y > 0) {
            jumpCmp.wantsToJump = true
            jumpCmp.jumpVelocity = vel.y * JUMP_CUT_FACTOR
        }
    }

    private fun calculateJumpBuffer(
        intentionCmp: IntentionComponent,
        entity: Entity,
        jumpCmp: JumpComponent,
    ) {
        // calculate jumpBuffer
        if (intentionCmp.wantsToJump &&
            entity hasNo HasGroundContact
        ) {
            jumpCmp.resetJumpBuffer()
        }

        if (jumpCmp.jumpBuffer > 0f) {
            jumpCmp.jumpBuffer -= deltaTime
        }
    }

    // formula taken from: https://www.iforce2d.net/b2dtut/projected-trajectory
    private fun getJumpVelocity(desiredHeight: Float): Float {
        val gravityPerStep = Vector2()
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

    companion object {
        val logger = ktx.log.logger<JumpSystem>()
    }
}
