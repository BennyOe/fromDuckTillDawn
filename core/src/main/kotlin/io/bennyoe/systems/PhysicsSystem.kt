package io.bennyoe.systems

import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType.StaticBody
import com.badlogic.gdx.physics.box2d.Contact
import com.badlogic.gdx.physics.box2d.ContactImpulse
import com.badlogic.gdx.physics.box2d.ContactListener
import com.badlogic.gdx.physics.box2d.Manifold
import com.badlogic.gdx.physics.box2d.World
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.Fixed
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import com.github.quillraven.fleks.World.Companion.inject
import io.bennyoe.Duckee
import io.bennyoe.components.AnimationComponent
import io.bennyoe.components.BashComponent
import io.bennyoe.components.HasGroundContact
import io.bennyoe.components.ImageComponent
import io.bennyoe.components.JumpComponent
import io.bennyoe.components.MoveComponent
import io.bennyoe.components.PhysicComponent
import ktx.log.logger
import ktx.math.component1
import ktx.math.component2

class PhysicsSystem(
    private val phyWorld: World = inject("phyWorld"),
) : IteratingSystem(family { all(PhysicComponent, ImageComponent) }, interval = Fixed(Duckee.PHYSIC_TIME_STEP)), ContactListener {
    private var activeGroundContacts: Int = 0

    init {
        phyWorld.setContactListener(this)
    }

    override fun onUpdate() {
        if (phyWorld.autoClearForces) {
            logger.error { "AutoClearForces must be set to false" }
            phyWorld.autoClearForces = false
        }
        super.onUpdate()
        phyWorld.clearForces()
    }

    override fun onTick() {
        super.onTick()
        phyWorld.step(deltaTime, 6, 2)
    }

    override fun onTickEntity(entity: Entity) {
        val physicCmp = entity[PhysicComponent]
        val moveCmp = entity.getOrNull(MoveComponent)
        val jumpCmp = entity.getOrNull(JumpComponent)
        val bashCmp = entity.getOrNull(BashComponent)
        val animationCmp = entity[AnimationComponent]

        setJumpImpulse(jumpCmp, physicCmp, entity)
        setWalkImpulse(moveCmp, physicCmp)
        setBashImpulse(bashCmp, animationCmp, physicCmp, entity)
        setGroundContact(entity)

        physicCmp.prevPos.set(physicCmp.body.position)

        if (!physicCmp.impulse.isZero) {
            physicCmp.body.applyLinearImpulse(physicCmp.impulse, physicCmp.body.worldCenter, true)
            physicCmp.impulse.setZero()
        }
    }

    private fun setBashImpulse(
        bashCmp: BashComponent?,
        animationCmp: AnimationComponent,
        physicCmp: PhysicComponent,
        entity: Entity
    ) {
        bashCmp?.let {
            val inverse = if (animationCmp.flipImage) -1 else 1
            physicCmp.impulse.x = inverse * physicCmp.body.mass * bashCmp.bashPower
            if (bashCmp.bashCooldown <= 0) {
                entity.configure { it -= BashComponent }
            } else {
                bashCmp.bashCooldown -= deltaTime
            }
        }
    }

    private fun setWalkImpulse(moveCmp: MoveComponent?, physicCmp: PhysicComponent) {
        moveCmp?.let {
            physicCmp.impulse.x = physicCmp.body.mass * (moveCmp.moveVelocity - physicCmp.body.linearVelocity.x)
        }
    }

    private fun setJumpImpulse(
        jumpCmp: JumpComponent?,
        physicCmp: PhysicComponent,
        entity: Entity
    ) {
        jumpCmp?.let { jump ->
            physicCmp.impulse.y = physicCmp.body.mass * (jump.jumpVelocity - physicCmp.body.linearVelocity.y)
            entity.configure { it -= JumpComponent }
        }
    }

    private fun setGroundContact(playerEntity: Entity) {
        if (activeGroundContacts > 0) {
            playerEntity.configure {
                it += HasGroundContact
            }
        } else {
            playerEntity.configure {
                it -= HasGroundContact
            }
        }
    }

    // alpha is the offset between two frames
    // this is for interpolating the animation
    override fun onAlphaEntity(entity: Entity, alpha: Float) {
        val imageCmp = entity[ImageComponent]
        val physicCmp = entity[PhysicComponent]

        val (prevX, prevY) = physicCmp.prevPos
        val (bodyX, bodyY) = physicCmp.body.position
        imageCmp.image.run {
            setPosition(
                MathUtils.lerp(prevX, bodyX, alpha) - width * 0.5f,
                MathUtils.lerp(prevY, bodyY, alpha) - height * 0.5f
            )
        }
    }

    override fun beginContact(contact: Contact) {
        if (hasGroundContact(contact)) {
            activeGroundContacts++
        }
    }


    override fun endContact(contact: Contact) {
        if ((hasGroundContact(contact))
        ) {
            activeGroundContacts--
        }
    }

    override fun preSolve(contact: Contact, oldManifold: Manifold) {
        // here you can check if the type is dynamic or static and decide which are going to collide (contact.fixture.body.type)
        contact.isEnabled = true
    }

    override fun postSolve(contact: Contact, impulse: ContactImpulse) {
    }

    private fun hasGroundContact(contact: Contact): Boolean {
        return (contact.fixtureA.body.type == StaticBody && contact.fixtureB.userData == "GROUND_COLLISION" ||
            contact.fixtureB.body.type == StaticBody && contact.fixtureA.userData == "GROUND_COLLISION")
    }

    companion object {
        private val logger = logger<PhysicsSystem>()
    }
}
