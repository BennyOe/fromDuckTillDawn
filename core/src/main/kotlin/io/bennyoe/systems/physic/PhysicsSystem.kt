package io.bennyoe.systems.physic

import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.physics.box2d.World
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.Fixed
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import com.github.quillraven.fleks.World.Companion.inject
import io.bennyoe.components.BashComponent
import io.bennyoe.components.HasGroundContact
import io.bennyoe.components.HealthComponent
import io.bennyoe.components.ImageComponent
import io.bennyoe.components.JumpComponent
import io.bennyoe.components.MoveComponent
import io.bennyoe.components.PhysicComponent
import io.bennyoe.config.GameConstants
import io.bennyoe.systems.PausableSystem
import ktx.log.logger
import ktx.math.component1
import ktx.math.component2

class PhysicsSystem(
    private val phyWorld: World = inject("phyWorld"),
) : IteratingSystem(
        family { all(PhysicComponent, ImageComponent) },
        interval =
            Fixed(
                GameConstants.PHYSIC_TIME_STEP,
            ),
    ),
    PausableSystem {
    override fun onUpdate() {
        if (phyWorld.autoClearForces) {
            logger.error { "AutoClearForces must be set to false" }
            phyWorld.autoClearForces = false
        }
        super.onUpdate()
    }

    override fun onTick() {
        super.onTick()
        phyWorld.step(deltaTime, 6, 2)
        phyWorld.clearForces()
    }

    override fun onTickEntity(entity: Entity) {
        val physicCmp = entity[PhysicComponent]
        val moveCmp = entity.getOrNull(MoveComponent)
        val jumpCmp = entity.getOrNull(JumpComponent)
        val bashCmp = entity.getOrNull(BashComponent)
        val imageCmp = entity[ImageComponent]
        val healthCmp = entity[HealthComponent]

        setJumpImpulse(jumpCmp, physicCmp)
        setWalkImpulse(moveCmp, physicCmp)
        setBashImpulse(bashCmp, imageCmp, physicCmp, entity)
        setGroundContact(entity)
        if (moveCmp != null && (moveCmp.throwBack || moveCmp.throwBackCooldown > 0)) {
            setThrowBackImpulse(moveCmp, physicCmp, healthCmp)
        }

        physicCmp.prevPos.set(physicCmp.body.position)

        if (!physicCmp.impulse.isZero) {
            physicCmp.body.applyLinearImpulse(physicCmp.impulse, physicCmp.body.worldCenter, true)
            physicCmp.impulse.setZero()
        }
    }

    // alpha is the offset between two frames
    // this is for interpolating the animation
    override fun onAlphaEntity(
        entity: Entity,
        alpha: Float,
    ) {
        val imageCmp = entity[ImageComponent]
        val physicCmp = entity[PhysicComponent]

        val (prevX, prevY) = physicCmp.prevPos
        val (bodyX, bodyY) = physicCmp.body.position
        imageCmp.image.run {
            setPosition(
                MathUtils.lerp(prevX, bodyX, alpha) - width * 0.5f,
                MathUtils.lerp(prevY, bodyY, alpha) - height * 0.5f,
            )
        }
    }

    private fun setBashImpulse(
        bashCmp: BashComponent?,
        imageCmp: ImageComponent,
        physicCmp: PhysicComponent,
        entity: Entity,
    ) {
        bashCmp?.let {
            val inverse = if (imageCmp.flipImage) -1 else 1
            physicCmp.impulse.x = inverse * physicCmp.body.mass * bashCmp.bashPower
            if (bashCmp.bashCooldown <= 0) {
                entity.configure { it -= BashComponent }
            } else {
                bashCmp.bashCooldown -= deltaTime
            }
        }
    }

    private fun setThrowBackImpulse(
        moveCmp: MoveComponent?,
        physicCmp: PhysicComponent,
        healthComponent: HealthComponent,
    ) {
        moveCmp?.let {
            if (it.throwBack) {
                val inverse = if (healthComponent.attackedFromBehind) 1 else -1
                physicCmp.impulse.x = inverse * physicCmp.body.mass * 10f
                it.throwBackCooldown = 0.2f
                it.throwBack = false
            }
            if (it.throwBackCooldown > 0) {
                it.throwBackCooldown -= deltaTime
                moveCmp.moveVelocity = 0f
            }
        }
    }

    private fun setWalkImpulse(
        moveCmp: MoveComponent?,
        physicCmp: PhysicComponent,
    ) {
        moveCmp?.let {
            if (it.throwBackCooldown > 0) return
            physicCmp.impulse.x = physicCmp.body.mass * (moveCmp.moveVelocity - physicCmp.body.linearVelocity.x)
        }
    }

    private fun setJumpImpulse(
        jumpCmp: JumpComponent?,
        physicCmp: PhysicComponent,
    ) {
        jumpCmp?.let { jump ->
            if (jumpCmp.wantsToJump) {
                physicCmp.impulse.y = physicCmp.body.mass * (jump.jumpVelocity - physicCmp.body.linearVelocity.y)
                jumpCmp.wantsToJump = false
            }
        }
    }

    private fun setGroundContact(playerEntity: Entity) {
        val physicCmp = playerEntity[PhysicComponent]
        if (physicCmp.activeGroundContacts > 0) {
            playerEntity.configure {
                it += HasGroundContact
            }
        } else {
            playerEntity.configure {
                it -= HasGroundContact
            }
        }
    }

    companion object {
        private val logger = logger<PhysicsSystem>()
    }
}
