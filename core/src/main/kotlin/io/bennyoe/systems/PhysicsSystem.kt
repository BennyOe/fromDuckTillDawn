package io.bennyoe.systems

import com.badlogic.gdx.math.MathUtils
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
import io.bennyoe.components.AnimationComponent
import io.bennyoe.components.BashComponent
import io.bennyoe.components.HasGroundContact
import io.bennyoe.components.ImageComponent
import io.bennyoe.components.JumpComponent
import io.bennyoe.components.MoveComponent
import io.bennyoe.components.PhysicComponent
import io.bennyoe.components.ai.NearbyEnemiesComponent
import io.bennyoe.config.EntityCategory
import io.bennyoe.config.GameConstants.PHYSIC_TIME_STEP
import io.bennyoe.utility.BodyData
import io.bennyoe.utility.FixtureType
import io.bennyoe.utility.bodyData
import io.bennyoe.utility.fixtureData
import ktx.log.logger
import ktx.math.component1
import ktx.math.component2

class PhysicsSystem(
    private val phyWorld: World = inject("phyWorld"),
) : IteratingSystem(family { all(PhysicComponent, ImageComponent) }, interval = Fixed(PHYSIC_TIME_STEP)),
    ContactListener {
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

        setJumpImpulse(jumpCmp, physicCmp)
        setWalkImpulse(moveCmp, physicCmp)
        setBashImpulse(bashCmp, animationCmp, physicCmp, entity)
        setGroundContact(entity)

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

    override fun beginContact(contact: Contact) {
        val dataA: BodyData? = contact.fixtureA.bodyData
        val dataB: BodyData? = contact.fixtureB.bodyData
        if (hasGroundContact(contact)) {
            if (dataA?.type == EntityCategory.PLAYER) {
                dataA.entity.getOrNull(PhysicComponent)?.let { it.activeGroundContacts++ }
            }
            if (dataB?.type == EntityCategory.PLAYER) {
                dataB.entity.getOrNull(PhysicComponent)?.let { it.activeGroundContacts++ }
            }
        }

        if (checkForNearbyEnemies(contact)) {
            // Hole Entity & AIComponent nur, wenn zutreffend
            val fixtureA = contact.fixtureA
            val fixtureB = contact.fixtureB
            val fixtureDataA = fixtureA.fixtureData
            val fixtureDataB = fixtureB.fixtureData
            val bodyDataA = fixtureA.bodyData
            val bodyDataB = fixtureB.bodyData

            if (fixtureDataA?.type == FixtureType.NEARBY_ENEMY_SENSOR && bodyDataB?.type == EntityCategory.PLAYER) {
                // Entity mit Sensor bekommt den Enemy in nearbyEntities
                val nearbyEnemiesCmp = bodyDataA?.entity?.getOrNull(NearbyEnemiesComponent)
                nearbyEnemiesCmp?.nearbyEntities += bodyDataB.entity
                logger.debug { "Nearby Entities: ${nearbyEnemiesCmp?.nearbyEntities}" }
            }
            if (fixtureDataB?.type == FixtureType.NEARBY_ENEMY_SENSOR && bodyDataA?.type == EntityCategory.PLAYER) {
                val nearbyEnemiesCmp = bodyDataB?.entity?.getOrNull(NearbyEnemiesComponent)
                nearbyEnemiesCmp?.nearbyEntities += bodyDataA.entity
                logger.debug { "Nearby Entities: ${nearbyEnemiesCmp?.nearbyEntities}" }
            }
        }
    }

    override fun endContact(contact: Contact) {
        val fixtureA = contact.fixtureA
        val fixtureB = contact.fixtureB
        val fixtureDataA = fixtureA.fixtureData
        val fixtureDataB = fixtureB.fixtureData
        val bodyDataA = fixtureA.bodyData
        val bodyDataB = fixtureB.bodyData

        if (hasGroundContact(contact)) {
            if (bodyDataA?.type == EntityCategory.PLAYER) {
                bodyDataA.entity.getOrNull(PhysicComponent)?.let { it.activeGroundContacts-- }
            }
            if (bodyDataB?.type == EntityCategory.PLAYER) {
                bodyDataB.entity.getOrNull(PhysicComponent)?.let { it.activeGroundContacts-- }
            }
        }
        if (fixtureDataA?.type == FixtureType.NEARBY_ENEMY_SENSOR && bodyDataB?.type == EntityCategory.PLAYER) {
            // Entity mit Sensor bekommt den Enemy in nearbyEntities
            val nearbyEnemiesCmp = bodyDataA?.entity?.getOrNull(NearbyEnemiesComponent)
            nearbyEnemiesCmp?.nearbyEntities -= bodyDataB.entity
            logger.debug { "Nearby Entities: ${nearbyEnemiesCmp?.nearbyEntities}" }
        }
        if (fixtureDataB?.type == FixtureType.NEARBY_ENEMY_SENSOR && bodyDataA?.type == EntityCategory.PLAYER) {
            val nearbyEnemiesCmp = bodyDataB?.entity?.getOrNull(NearbyEnemiesComponent)
            nearbyEnemiesCmp?.nearbyEntities -= bodyDataA.entity
            logger.debug { "Nearby Entities: ${nearbyEnemiesCmp?.nearbyEntities}" }
        }
    }

    override fun preSolve(
        contact: Contact,
        oldManifold: Manifold,
    ) {
        // here you can check if the type is dynamic or static and decide which are going to collide (contact.fixture.body.type)
        contact.isEnabled = true
    }

    override fun postSolve(
        contact: Contact,
        impulse: ContactImpulse,
    ) {
    }

    private fun setBashImpulse(
        bashCmp: BashComponent?,
        animationCmp: AnimationComponent,
        physicCmp: PhysicComponent,
        entity: Entity,
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

    private fun setWalkImpulse(
        moveCmp: MoveComponent?,
        physicCmp: PhysicComponent,
    ) {
        moveCmp?.let {
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

    private fun hasGroundContact(contact: Contact): Boolean {
        val fixtureA = contact.fixtureA
        val fixtureB = contact.fixtureB

        val dataA = fixtureA.fixtureData
        val dataB = fixtureB.fixtureData

        val bodyDataA = fixtureA.bodyData
        val bodyDataB = fixtureB.bodyData

        val isAOnGround = dataA?.type == FixtureType.GROUND_SENSOR && bodyDataB?.type == EntityCategory.GROUND
        val isBOnGround = dataB?.type == FixtureType.GROUND_SENSOR && bodyDataA?.type == EntityCategory.GROUND

        return isAOnGround || isBOnGround
    }

    private fun checkForNearbyEnemies(contact: Contact): Boolean {
        val fixtureA = contact.fixtureA
        val fixtureB = contact.fixtureB

        val fixtureDataA = fixtureA.fixtureData
        val fixtureDataB = fixtureB.fixtureData
        val bodyDataA = fixtureA.bodyData
        val bodyDataB = fixtureB.bodyData

        // NearbyEnemy = wenn ein NEARBY_ENEMY_SENSOR ein Fixture berührt, das zu einem Enemy gehört
        val isAEnemySensor = fixtureDataA?.type == FixtureType.NEARBY_ENEMY_SENSOR
        val isBEnemySensor = fixtureDataB?.type == FixtureType.NEARBY_ENEMY_SENSOR

        val isAEnemy = bodyDataA?.type == EntityCategory.PLAYER
        val isBEnemy = bodyDataB?.type == EntityCategory.PLAYER

        // True, wenn Sensor einen Gegner berührt
        return (isAEnemySensor && isBEnemy) || (isBEnemySensor && isAEnemy)
    }

    companion object {
        private val logger = logger<PhysicsSystem>()
    }
}
