package io.bennyoe.systems.physic

import com.badlogic.gdx.physics.box2d.Contact
import com.badlogic.gdx.physics.box2d.ContactImpulse
import com.badlogic.gdx.physics.box2d.ContactListener
import com.badlogic.gdx.physics.box2d.Fixture
import com.badlogic.gdx.physics.box2d.Manifold
import com.badlogic.gdx.physics.box2d.World
import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.quillraven.fleks.IntervalSystem
import com.github.quillraven.fleks.World.Companion.inject
import io.bennyoe.components.AttackComponent
import io.bennyoe.components.GroundTypeSensorComponent
import io.bennyoe.components.HealthComponent
import io.bennyoe.components.PhysicComponent
import io.bennyoe.components.ai.NearbyEnemiesComponent
import io.bennyoe.components.audio.AmbienceSoundComponent
import io.bennyoe.components.audio.ReverbZoneComponent
import io.bennyoe.components.audio.ReverbZoneContactComponent
import io.bennyoe.components.audio.SoundTriggerComponent
import io.bennyoe.config.EntityCategory
import io.bennyoe.event.AmbienceChangeEvent
import io.bennyoe.event.PlaySoundEvent
import io.bennyoe.event.StreamSoundEvent
import io.bennyoe.event.fire
import io.bennyoe.systems.PausableSystem
import io.bennyoe.utility.BodyData
import io.bennyoe.utility.SensorType
import io.bennyoe.utility.bodyData
import io.bennyoe.utility.fixtureData
import ktx.log.logger

class ContactHandlerSystem(
    private val stage: Stage = inject("stage"),
    phyWorld: World = inject("phyWorld"),
) : IntervalSystem(),
    ContactListener,
    PausableSystem {
    init {
        phyWorld.setContactListener(this)
    }

    override fun onTick() {}

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
        val (_, soundTriggerBodyData) =
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
        val (_, ambienceSoundBodyData) =
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
            if (playerBodyData.entity has ReverbZoneContactComponent.Companion &&
                reverbZoneBodyData.entity has ReverbZoneComponent.Companion
            ) {
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

    companion object {
        private val logger = logger<ContactHandlerSystem>()
    }
}
