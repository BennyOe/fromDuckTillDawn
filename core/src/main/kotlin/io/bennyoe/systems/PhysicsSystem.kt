package io.bennyoe.systems

import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType.DynamicBody
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
import io.bennyoe.components.HasGroundContact
import io.bennyoe.components.ImageComponent
import io.bennyoe.components.PhysicComponent
import ktx.log.logger
import ktx.math.component1
import ktx.math.component2

class PhysicsSystem(
    private val phyWorld: World = inject("phyWorld"),
) : IteratingSystem(family { all(PhysicComponent, ImageComponent) }, interval = Fixed(1 / 60f)), ContactListener {

    init {
        phyWorld.setContactListener(this)
    }

    override fun onUpdate() {
        if (phyWorld.autoClearForces) {
            LOG.error { "AutoClearForces must be set to false" }
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
        physicCmp.prevPos.set(physicCmp.body.position)

        if (!physicCmp.impulse.isZero) {
            physicCmp.body.applyLinearImpulse(physicCmp.impulse, physicCmp.body.worldCenter, true)
            physicCmp.impulse.setZero()
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
        if ((contact.fixtureA.body.type == StaticBody && contact.fixtureB.body.type == DynamicBody)
        ) {
            val entity = contact.fixtureB.body.userData as? Entity ?: return
            val normal = contact.worldManifold.normal

            // check, if collision is on bottom of player (stands on ground)
            if (normal.y > 0) {
                entity.configure {
                    it += HasGroundContact
                }
            }
        }
    }

    override fun endContact(contact: Contact) {
        if ((contact.fixtureA.body.type == StaticBody && contact.fixtureB.body.type == DynamicBody)
        ) {
            val entity = contact.fixtureB.body.userData as? Entity ?: return
            entity.configure {
                it -= HasGroundContact
            }
        }
    }

    override fun preSolve(contact: Contact, oldManifold: Manifold) {
        // here you can check if the type is dynamic or static and decide which are going to collide (contact.fixture.body.type)
        contact.isEnabled = true
    }

    override fun postSolve(contact: Contact, impulse: ContactImpulse) {
    }

    companion object {
        private val LOG = logger<PhysicsSystem>()
    }
}
