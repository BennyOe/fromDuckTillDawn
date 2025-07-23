package io.bennyoe.systems

import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.physics.box2d.Contact
import com.badlogic.gdx.physics.box2d.ContactImpulse
import com.badlogic.gdx.physics.box2d.ContactListener
import com.badlogic.gdx.physics.box2d.Fixture
import com.badlogic.gdx.physics.box2d.Manifold
import com.badlogic.gdx.physics.box2d.World
import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.Fixed
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import com.github.quillraven.fleks.World.Companion.inject
import io.bennyoe.components.AttackComponent
import io.bennyoe.components.BashComponent
import io.bennyoe.components.GroundTypeSensorComponent
import io.bennyoe.components.HasGroundContact
import io.bennyoe.components.HealthComponent
import io.bennyoe.components.ImageComponent
import io.bennyoe.components.JumpComponent
import io.bennyoe.components.MoveComponent
import io.bennyoe.components.PhysicComponent
import io.bennyoe.components.ai.NearbyEnemiesComponent
import io.bennyoe.components.audio.AmbienceSoundComponent
import io.bennyoe.components.audio.ReverbZoneComponent
import io.bennyoe.components.audio.ReverbZoneContactComponent
import io.bennyoe.components.audio.SoundTriggerComponent
import io.bennyoe.config.EntityCategory
import io.bennyoe.config.GameConstants.PHYSIC_TIME_STEP
import io.bennyoe.event.AmbienceChangeEvent
import io.bennyoe.event.PlaySoundEvent
import io.bennyoe.event.StreamSoundEvent
import io.bennyoe.event.fire
import io.bennyoe.utility.BodyData
import io.bennyoe.utility.SensorType
import io.bennyoe.utility.bodyData
import io.bennyoe.utility.fixtureData
import ktx.log.logger
import ktx.math.component1
import ktx.math.component2

class PhysicsSystem(
    private val phyWorld: World = inject("phyWorld"),
    private val stage: Stage = inject("stage"),
) : IteratingSystem(family { all(PhysicComponent, ImageComponent) }, interval = Fixed(PHYSIC_TIME_STEP)),
    ContactListener,
    PausableSystem {
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

    override fun beginContact(contact: Contact) {
        val bodyDataA = contact.fixtureA.bodyData ?: return
        val bodyDataB = contact.fixtureB.bodyData ?: return

        handleReverbZoneContactBegin(bodyDataA, bodyDataB, contact.fixtureA, contact.fixtureB)
        handleSoundTriggerContactBegin(bodyDataA, bodyDataB, contact.fixtureA, contact.fixtureB)
        handleAmbienceSoundContactBegin(bodyDataA, bodyDataB, contact.fixtureA, contact.fixtureB)
        handleGroundContactBegin(bodyDataA, bodyDataB, contact.fixtureA, contact.fixtureB)
        handleNearbyEnemiesBegin(bodyDataA, bodyDataB, contact.fixtureA, contact.fixtureB)
        handlePlayerEnemyCollision(bodyDataA, bodyDataB, contact.fixtureA, contact.fixtureB)
        handleGroundTypeBegin(bodyDataA, bodyDataB, contact.fixtureA, contact.fixtureB)
    }

    override fun endContact(contact: Contact) {
        val bodyDataA = contact.fixtureA.bodyData ?: return
        val bodyDataB = contact.fixtureB.bodyData ?: return

        handleReverbZoneContactEnd(bodyDataA, bodyDataB, contact.fixtureA, contact.fixtureB)
        handleGroundContactEnd(bodyDataA, bodyDataB, contact.fixtureA, contact.fixtureB)
        handleNearbyEnemiesEnd(bodyDataA, bodyDataB, contact.fixtureA, contact.fixtureB)
    }

    override fun preSolve(
        contact: Contact,
        oldManifold: Manifold,
    ) {
        val bodyDataA = contact.fixtureA.bodyData ?: return
        val bodyDataB = contact.fixtureB.bodyData ?: return

        // Player and Enemy should not physically collide, only their sensors.
        val isPlayerEnemyHitboxCollision =
            (bodyDataA.type == EntityCategory.PLAYER && bodyDataB.type == EntityCategory.ENEMY) ||
                (bodyDataA.type == EntityCategory.ENEMY && bodyDataB.type == EntityCategory.PLAYER)
        if (isPlayerEnemyHitboxCollision) {
            contact.isEnabled = false
        }
    }

    override fun postSolve(
        contact: Contact,
        impulse: ContactImpulse,
    ) {
    }

    private fun handleSoundTriggerContactBegin(
        bodyDataA: BodyData,
        bodyDataB: BodyData,
        fixtureA: Fixture,
        fixtureB: Fixture,
    ) {
        val (playerBodyData, soundTriggerBodyData) =
            when {
                fixtureA.fixtureData?.type == SensorType.SOUND_TRIGGER_SENSOR && bodyDataB.type == EntityCategory.PLAYER ->
                    bodyDataB to bodyDataA

                fixtureB.fixtureData?.type == SensorType.SOUND_TRIGGER_SENSOR && bodyDataA.type == EntityCategory.PLAYER ->
                    bodyDataA to bodyDataB

                else -> return
            }

        val soundTriggerCmp = soundTriggerBodyData.entity[SoundTriggerComponent]
        val physicCmp = soundTriggerBodyData.entity[PhysicComponent]

        if (soundTriggerCmp.streamed) {
            logger.debug { "SoundStream Event fired at position ${physicCmp.body.position}" }
            stage.fire(
                StreamSoundEvent(
                    soundTriggerBodyData.entity,
                    soundTriggerCmp.sound!!,
                    soundTriggerCmp.volume,
                    physicCmp.body.position,
                ),
            )
        } else {
            // TODO not usable atm. Have to think about how preloaded sounds should be handled since other sounds are managed with a
            //  SoundProfileComponent.
            logger.debug { "SoundType Event fired" }
            stage.fire(
                PlaySoundEvent(
                    soundTriggerBodyData.entity,
                    soundTriggerCmp.type!!,
                    soundTriggerCmp.volume,
                    physicCmp.body.position,
                ),
            )
        }
    }

    private fun handleAmbienceSoundContactBegin(
        bodyDataA: BodyData,
        bodyDataB: BodyData,
        fixtureA: Fixture,
        fixtureB: Fixture,
    ) {
        val (playerBodyData, ambienceSoundBodyData) =
            when {
                fixtureA.fixtureData?.type == SensorType.SOUND_AMBIENCE_SENSOR && bodyDataB.type == EntityCategory.PLAYER ->
                    bodyDataB to bodyDataA

                fixtureB.fixtureData?.type == SensorType.SOUND_AMBIENCE_SENSOR && bodyDataA.type == EntityCategory.PLAYER ->
                    bodyDataA to bodyDataB

                else -> return
            }

        val ambienceSoundCmp = ambienceSoundBodyData.entity[AmbienceSoundComponent]

        logger.debug { "Ambience Event ${ambienceSoundCmp.sound} played" }
        stage.fire(
            AmbienceChangeEvent(
                ambienceSoundCmp.type,
                ambienceSoundCmp.sound,
                ambienceSoundCmp.volume!!,
            ),
        )
    }

    private fun handleReverbZoneContactBegin(
        bodyDataA: BodyData,
        bodyDataB: BodyData,
        fixtureA: Fixture,
        fixtureB: Fixture,
    ) {
        val (playerBodyData, reverbZoneBodyData) =
            when {
                fixtureA.fixtureData?.type == SensorType.AUDIO_EFFECT_SENSOR && bodyDataB.type == EntityCategory.PLAYER ->
                    bodyDataB to bodyDataA

                fixtureB.fixtureData?.type == SensorType.AUDIO_EFFECT_SENSOR && bodyDataA.type == EntityCategory.PLAYER ->
                    bodyDataA to bodyDataB

                else -> return
            }

        val reverbZoneCmp = reverbZoneBodyData.entity[ReverbZoneComponent]
        val reverbZoneContactCmp = playerBodyData.entity[ReverbZoneContactComponent]

        reverbZoneContactCmp.increaseContact(reverbZoneCmp)
    }

    private fun handleReverbZoneContactEnd(
        bodyDataA: BodyData,
        bodyDataB: BodyData,
        fixtureA: Fixture,
        fixtureB: Fixture,
    ) {
        val (playerBodyData, reverbZoneBodyData) =
            when {
                fixtureA.fixtureData?.type == SensorType.AUDIO_EFFECT_SENSOR &&
                    bodyDataB.type == EntityCategory.PLAYER &&
                    fixtureB.fixtureData?.type == SensorType.HITBOX_SENSOR -> {
                    bodyDataB to bodyDataA
                }

                fixtureB.fixtureData?.type == SensorType.AUDIO_EFFECT_SENSOR &&
                    bodyDataA.type == EntityCategory.PLAYER &&
                    fixtureA.fixtureData?.type == SensorType.HITBOX_SENSOR -> {
                    bodyDataA to bodyDataB
                }

                else -> return
            }

        with(world) {
            if (playerBodyData.entity has ReverbZoneContactComponent && reverbZoneBodyData.entity has ReverbZoneComponent) {
                val reverbZoneCmp = reverbZoneBodyData.entity[ReverbZoneComponent]
                val reverbZoneContactCmp = playerBodyData.entity[ReverbZoneContactComponent]

                reverbZoneContactCmp.decreaseContact(reverbZoneCmp)
            }
        }
    }

    private fun handleGroundTypeBegin(
        bodyDataA: BodyData,
        bodyDataB: BodyData,
        fixtureA: Fixture,
        fixtureB: Fixture,
    ) {
        val (entityBodyData, groundBodyData) =
            when {
                fixtureA.fixtureData?.type == SensorType.GROUND_TYPE_SENSOR && bodyDataB.type == EntityCategory.GROUND ->
                    bodyDataA to
                        bodyDataB

                fixtureB.fixtureData?.type == SensorType.GROUND_TYPE_SENSOR && bodyDataA.type == EntityCategory.GROUND ->
                    bodyDataB to
                        bodyDataA

                else -> return
            }

        with(world) {
            if (entityBodyData.entity has GroundTypeSensorComponent) {
                entityBodyData.entity[PhysicComponent].floorType = groundBodyData.floorType
            }
        }
    }

    private fun handleGroundContactBegin(
        bodyDataA: BodyData,
        bodyDataB: BodyData,
        fixtureA: Fixture,
        fixtureB: Fixture,
    ) {
        val playerBodyData =
            when {
                fixtureA.fixtureData?.type == SensorType.GROUND_DETECT_SENSOR && bodyDataB.type == EntityCategory.GROUND -> bodyDataA
                fixtureB.fixtureData?.type == SensorType.GROUND_DETECT_SENSOR && bodyDataA.type == EntityCategory.GROUND -> bodyDataB
                else -> return
            }

        if (playerBodyData.type == EntityCategory.PLAYER) {
            with(world) {
                playerBodyData.entity[PhysicComponent].activeGroundContacts++
            }
        }
    }

    private fun handleGroundContactEnd(
        bodyDataA: BodyData,
        bodyDataB: BodyData,
        fixtureA: Fixture,
        fixtureB: Fixture,
    ) {
        val playerBodyData =
            when {
                fixtureA.fixtureData?.type == SensorType.GROUND_DETECT_SENSOR && bodyDataB.type == EntityCategory.GROUND -> bodyDataA
                fixtureB.fixtureData?.type == SensorType.GROUND_DETECT_SENSOR && bodyDataA.type == EntityCategory.GROUND -> bodyDataB
                else -> return
            }

        if (playerBodyData.type == EntityCategory.PLAYER) {
            with(world) {
                if (playerBodyData.entity has PhysicComponent) {
                    playerBodyData.entity[PhysicComponent].activeGroundContacts--
                }
            }
        }
    }

    private fun handleNearbyEnemiesBegin(
        bodyDataA: BodyData,
        bodyDataB: BodyData,
        fixtureA: Fixture,
        fixtureB: Fixture,
    ) {
        val (sensorBodyData, targetBodyData) =
            when {
                fixtureA.fixtureData?.type == SensorType.NEARBY_ENEMY_SENSOR && bodyDataB.type == EntityCategory.PLAYER ->
                    bodyDataA to
                        bodyDataB

                fixtureB.fixtureData?.type == SensorType.NEARBY_ENEMY_SENSOR && bodyDataA.type == EntityCategory.PLAYER ->
                    bodyDataB to
                        bodyDataA

                else -> return
            }

        with(world) {
            sensorBodyData.entity[NearbyEnemiesComponent].nearbyEntities += targetBodyData.entity
        }
    }

    private fun handleNearbyEnemiesEnd(
        bodyDataA: BodyData,
        bodyDataB: BodyData,
        fixtureA: Fixture,
        fixtureB: Fixture,
    ) {
        val (sensorBodyData, targetBodyData) =
            when {
                fixtureA.fixtureData?.type == SensorType.NEARBY_ENEMY_SENSOR && bodyDataB.type == EntityCategory.PLAYER ->
                    bodyDataA to
                        bodyDataB

                fixtureB.fixtureData?.type == SensorType.NEARBY_ENEMY_SENSOR && bodyDataA.type == EntityCategory.PLAYER ->
                    bodyDataB to
                        bodyDataA

                else -> return
            }

        with(world) {
            sensorBodyData.entity[NearbyEnemiesComponent].nearbyEntities -= targetBodyData.entity
        }
    }

    private fun handlePlayerEnemyCollision(
        bodyDataA: BodyData,
        bodyDataB: BodyData,
        fixtureA: Fixture,
        fixtureB: Fixture,
    ) {
        val isHitboxCollision =
            fixtureA.fixtureData?.type == SensorType.HITBOX_SENSOR && fixtureB.fixtureData?.type == SensorType.HITBOX_SENSOR
        if (!isHitboxCollision) return

        val (playerBodyData, enemyBodyData) =
            when {
                bodyDataA.type == EntityCategory.PLAYER && bodyDataB.type == EntityCategory.ENEMY -> bodyDataA to bodyDataB
                bodyDataA.type == EntityCategory.ENEMY && bodyDataB.type == EntityCategory.PLAYER -> bodyDataB to bodyDataA
                else -> return
            }

        with(world) {
            val attackCmp = enemyBodyData.entity[AttackComponent]
            val healthCmp = playerBodyData.entity[HealthComponent]
            healthCmp.attackedFromBehind =
                playerBodyData.entity[PhysicComponent]
                    .body.position.x >
                enemyBodyData.entity[PhysicComponent]
                    .body.position.x
            healthCmp.takenDamage = attackCmp.maxDamage
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
