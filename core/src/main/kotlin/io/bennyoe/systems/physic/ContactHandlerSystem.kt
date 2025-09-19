package io.bennyoe.systems.physic

import com.badlogic.gdx.physics.box2d.BodyDef
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
import io.bennyoe.components.WaterComponent
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
import io.bennyoe.utility.EntityBodyData
import io.bennyoe.utility.SensorType
import io.bennyoe.utility.bodyData
import io.bennyoe.utility.fixtureData
import ktx.log.logger

fun Fixture.hasSensorType(type: SensorType): Boolean = this.fixtureData?.sensorType == type

class ContactHandlerSystem(
    private val stage: Stage = inject("stage"),
    phyWorld: World = inject("phyWorld"),
) : IntervalSystem(),
    ContactListener,
    PausableSystem {
    init {
        phyWorld.setContactListener(this)
    }

    override fun onTick() = Unit

    // --- ContactListener -----------------------------------------------------

    override fun beginContact(contact: Contact) {
        val parts = contact.partsOrNull() ?: return

        handleReverbZoneBegin(parts)
        handleSoundTriggerBegin(parts)
        handleAmbienceBegin(parts)
        handleGroundBegin(parts)
        handleNearbyEnemiesBegin(parts)
        handlePlayerEnemyHitboxCollision(parts)
        handleGroundTypeBegin(parts)
        handleInWaterBegin(parts)
        handleUnderWaterBegin(parts)
    }

    override fun endContact(contact: Contact) {
        val parts = contact.partsOrNull() ?: return

        handleReverbZoneEnd(parts)
        handleGroundEnd(parts)
        handleNearbyEnemiesEnd(parts)
        handleInWaterEnd(parts)
        handleUnderWaterEnd(parts)
    }

    override fun preSolve(
        contact: Contact,
        oldManifold: Manifold,
    ) {
        val parts = contact.partsOrNull() ?: return
        if (parts.isPlayerEnemyCategories()) {
            // Player and Enemy bodies should not physically collide (sensors still fine).
            contact.isEnabled = false
        }
    }

    override fun postSolve(
        contact: Contact,
        impulse: ContactImpulse,
    ) = Unit

    // --- Handlers ------------------------------------------------------------

    private fun handleInWaterBegin(p: Parts) {
        val (waterFixture, objectFixture) = p.waterAndObjectFixturesOrNull() ?: return

        val waterBodyData = waterFixture.bodyData ?: return
        val objectPhysicCmp = objectFixture.bodyData?.entity?.getOrNull(PhysicComponent)

        val waterCmp = waterBodyData.entity[WaterComponent]
        val added = waterCmp.fixturePairs.add(waterFixture to objectFixture)

        if (added && objectPhysicCmp != null) {
            objectPhysicCmp.activeWaterContacts++
        }
    }

    private fun handleInWaterEnd(p: Parts) {
        val (waterFixture, objectFixture) = p.waterAndObjectFixturesOrNull() ?: return

        val waterBodyData = waterFixture.bodyData ?: return
        val objectPhysicCmp = objectFixture.bodyData?.entity?.getOrNull(PhysicComponent)

        val waterCmp = waterBodyData.entity[WaterComponent]

        with(world) {
            if (waterBodyData.entity has WaterComponent) {
                val removed = waterCmp.fixturePairs.remove(waterFixture to objectFixture)
                if (removed && objectPhysicCmp != null && objectPhysicCmp.activeWaterContacts > 0) {
                    objectPhysicCmp.activeWaterContacts--
                }
            }
        }
    }

    private fun handleUnderWaterBegin(p: Parts) {
        val (entityWithSensor, _) = p.entityAndUnderWaterWhenSensor(SensorType.UNDER_WATER_SENSOR) ?: return
        with(world) {
            entityWithSensor.entity.getOrNull(PhysicComponent)?.let {
                it.activeUnderWaterContacts++
            }
        }
    }

    private fun handleUnderWaterEnd(p: Parts) {
        val (entityWithSensor, _) = p.entityAndUnderWaterWhenSensor(SensorType.UNDER_WATER_SENSOR) ?: return
        with(world) {
            entityWithSensor.entity.getOrNull(PhysicComponent)?.let {
                if (it.activeUnderWaterContacts > 0) {
                    it.activeUnderWaterContacts--
                }
            }
        }
    }

    private fun handleSoundTriggerBegin(p: Parts) {
        // SOUND_TRIGGER_SENSOR touched by PLAYER
        val sensorOwner =
            p.sensorOwnerWhenPlayerTouches(SensorType.SOUND_TRIGGER_SENSOR) ?: return

        val soundTriggerCmp = sensorOwner.entity[SoundTriggerComponent]
        val physicCmp = sensorOwner.entity[PhysicComponent]

        if (soundTriggerCmp.streamed) {
            logger.debug { "SoundStream Event fired at position ${physicCmp.body.position}" }
            stage.fire(
                StreamSoundEvent(
                    sensorOwner.entity,
                    soundTriggerCmp.sound!!,
                    soundTriggerCmp.volume,
                    physicCmp.body.position,
                ),
            )
        } else {
            // NOTE: preloaded sounds currently not wired via SoundProfileComponent
            logger.debug { "SoundType Event fired" }
            stage.fire(
                PlaySoundEvent(
                    sensorOwner.entity,
                    soundTriggerCmp.type!!,
                    soundTriggerCmp.volume,
                    physicCmp.body.position,
                ),
            )
        }
    }

    private fun handleAmbienceBegin(p: Parts) {
        // SOUND_AMBIENCE_SENSOR touched by PLAYER
        val sensorOwner =
            p.sensorOwnerWhenPlayerTouches(SensorType.SOUND_AMBIENCE_SENSOR) ?: return

        val ambienceSoundCmp = sensorOwner.entity[AmbienceSoundComponent]
        logger.debug { "Ambience Event ${ambienceSoundCmp.type} played" }

        stage.fire(
            AmbienceChangeEvent(
                ambienceSoundCmp.type,
                ambienceSoundCmp.variations,
                ambienceSoundCmp.volume!!,
            ),
        )
    }

    private fun handleReverbZoneBegin(p: Parts) {
        // AUDIO_EFFECT_SENSOR touched by PLAYER
        val (player, reverbOwner) =
            p.playerAndSensorOwner(SensorType.AUDIO_EFFECT_SENSOR) ?: return

        val reverbZoneCmp = reverbOwner.entity[ReverbZoneComponent]
        val contactCmp = player.entity[ReverbZoneContactComponent]
        contactCmp.increaseContact(reverbZoneCmp)
    }

    private fun handleReverbZoneEnd(p: Parts) {
        // AUDIO_EFFECT_SENSOR no longer overlapping PLAYER (hitbox on player side)
        val pair =
            p.playerAndSensorOwnerOnEnd(
                sensorType = SensorType.AUDIO_EFFECT_SENSOR,
                playerSensor = SensorType.HITBOX_SENSOR,
            ) ?: return

        val (player, reverbOwner) = pair
        with(world) {
            if (player.entity has ReverbZoneContactComponent.Companion &&
                reverbOwner.entity has ReverbZoneComponent.Companion
            ) {
                val zone = reverbOwner.entity[ReverbZoneComponent]
                val contact = player.entity[ReverbZoneContactComponent]
                contact.decreaseContact(zone)
            }
        }
    }

    private fun handleGroundTypeBegin(p: Parts) {
        // GROUND_TYPE_SENSOR overlaps GROUND
        val (entityWithSensor, ground) = p.entityAndGroundWhenSensor(SensorType.GROUND_TYPE_SENSOR) ?: return
        with(world) {
            if (entityWithSensor.entity has GroundTypeSensorComponent) {
                entityWithSensor.entity[PhysicComponent].floorType = ground.floorType
            }
        }
    }

    private fun handleGroundBegin(p: Parts) {
        // GROUND_DETECT_SENSOR overlaps GROUND -> increase ground contacts if PLAYER
        val player = p.entityWithSensorWhenTouchingGround(SensorType.GROUND_DETECT_SENSOR) ?: return
        if (player.entityCategory == EntityCategory.PLAYER) {
            with(world) { player.entity[PhysicComponent].activeGroundContacts++ }
        }
    }

    private fun handleGroundEnd(p: Parts) {
        // GROUND_DETECT_SENSOR leaves GROUND -> decrease ground contacts if PLAYER
        val player = p.entityWithSensorWhenTouchingGround(SensorType.GROUND_DETECT_SENSOR) ?: return
        if (player.entityCategory == EntityCategory.PLAYER) {
            with(world) {
                if (player.entity has PhysicComponent) {
                    player.entity[PhysicComponent].activeGroundContacts--
                }
            }
        }
    }

    private fun handleNearbyEnemiesBegin(p: Parts) {
        // NEARBY_ENEMY_SENSOR on enemy detects PLAYER
        val (sensorOwner, player) =
            p.sensorOwnerAndPlayer(SensorType.NEARBY_ENEMY_SENSOR) ?: return

        with(world) {
            sensorOwner.entity[NearbyEnemiesComponent].nearbyEntities += player.entity
        }
    }

    private fun handleNearbyEnemiesEnd(p: Parts) {
        val (sensorOwner, player) =
            p.sensorOwnerAndPlayer(SensorType.NEARBY_ENEMY_SENSOR) ?: return

        with(world) {
            sensorOwner.entity[NearbyEnemiesComponent].nearbyEntities -= player.entity
        }
    }

    private fun handlePlayerEnemyHitboxCollision(p: Parts) {
        // Only when both fixtures are HITBOX_SENSOR and categories are PLAYER vs ENEMY
        if (!p.bothHitboxes()) return
        val pair = p.playerAndEnemyOrNull() ?: return

        val (player, enemy) = pair
        with(world) {
            val attack = enemy.entity[AttackComponent]
            val health = player.entity[HealthComponent]

            val playerX =
                player.entity[PhysicComponent]
                    .body.position.x
            val enemyX =
                enemy.entity[PhysicComponent]
                    .body.position.x

            health.attackedFromBehind = playerX > enemyX
            health.takenDamage = attack.maxDamage
        }
    }

    // --- Small helpers (reduce branching noise) ------------------------------

    private data class Parts(
        val aBody: EntityBodyData,
        val bBody: EntityBodyData,
        val aFixture: Fixture,
        val bFixture: Fixture,
    ) {
        val aCategory: EntityCategory = aBody.entityCategory
        val bCategory: EntityCategory = bBody.entityCategory

        fun isPlayerEnemyCategories(): Boolean =
            (aCategory == EntityCategory.PLAYER && bCategory == EntityCategory.ENEMY) ||
                (aCategory == EntityCategory.ENEMY && bCategory == EntityCategory.PLAYER)

        fun bothHitboxes(): Boolean = aFixture.hasSensorType(SensorType.HITBOX_SENSOR) && bFixture.hasSensorType(SensorType.HITBOX_SENSOR)

        fun playerAndEnemyOrNull(): Pair<EntityBodyData, EntityBodyData>? =
            when {
                aCategory == EntityCategory.PLAYER && bCategory == EntityCategory.ENEMY -> aBody to bBody
                aCategory == EntityCategory.ENEMY && bCategory == EntityCategory.PLAYER -> bBody to aBody
                else -> null
            }

        fun sensorOwnerWhenPlayerTouches(sensor: SensorType): EntityBodyData? =
            when {
                aFixture.hasSensorType(sensor) && bCategory == EntityCategory.PLAYER -> aBody
                bFixture.hasSensorType(sensor) && aCategory == EntityCategory.PLAYER -> bBody
                else -> null
            }

        fun playerAndSensorOwner(sensor: SensorType): Pair<EntityBodyData, EntityBodyData>? =
            when {
                aFixture.hasSensorType(sensor) && bCategory == EntityCategory.PLAYER -> bBody to aBody
                bFixture.hasSensorType(sensor) && aCategory == EntityCategory.PLAYER -> aBody to bBody
                else -> null
            }

        fun sensorOwnerAndPlayer(sensor: SensorType): Pair<EntityBodyData, EntityBodyData>? =
            when {
                aFixture.hasSensorType(sensor) && bCategory == EntityCategory.PLAYER -> aBody to bBody
                bFixture.hasSensorType(sensor) && aCategory == EntityCategory.PLAYER -> bBody to aBody
                else -> null
            }

        fun entityAndGroundWhenSensor(sensor: SensorType): Pair<EntityBodyData, EntityBodyData>? =
            when {
                aFixture.hasSensorType(sensor) && bCategory == EntityCategory.GROUND -> aBody to bBody
                bFixture.hasSensorType(sensor) && aCategory == EntityCategory.GROUND -> bBody to aBody
                else -> null
            }

        fun entityAndUnderWaterWhenSensor(sensor: SensorType): Pair<EntityBodyData, EntityBodyData>? =
            when {
                aFixture.hasSensorType(sensor) && bCategory == EntityCategory.WATER -> aBody to bBody
                bFixture.hasSensorType(sensor) && aCategory == EntityCategory.WATER -> bBody to aBody
                else -> null
            }

        fun entityWithSensorWhenTouchingGround(sensor: SensorType): EntityBodyData? = entityAndGroundWhenSensor(sensor)?.first

        fun playerAndSensorOwnerOnEnd(
            sensorType: SensorType,
            playerSensor: SensorType,
        ): Pair<EntityBodyData, EntityBodyData>? =
            when {
                aFixture.hasSensorType(sensorType) && bCategory == EntityCategory.PLAYER && bFixture.hasSensorType(playerSensor) ->
                    bBody to aBody

                bFixture.hasSensorType(sensorType) && aCategory == EntityCategory.PLAYER && aFixture.hasSensorType(playerSensor) ->
                    aBody to bBody

                else -> null
            }

        fun waterAndObjectFixturesOrNull(): Pair<Fixture, Fixture>? {
            val aIsWaterSensor = aFixture.hasSensorType(SensorType.IN_WATER_SENSOR)
            val bIsWaterSensor = bFixture.hasSensorType(SensorType.IN_WATER_SENSOR)

            // The player's main fixture has the HITBOX_SENSOR type
            val aIsObjectHitbox = aFixture.hasSensorType(SensorType.HITBOX_SENSOR) && aFixture.body.type == BodyDef.BodyType.DynamicBody
            val bIsObjectHitbox = bFixture.hasSensorType(SensorType.HITBOX_SENSOR) && bFixture.body.type == BodyDef.BodyType.DynamicBody

            return when {
                aIsWaterSensor && bIsObjectHitbox -> aFixture to bFixture
                bIsWaterSensor && aIsObjectHitbox -> bFixture to aFixture
                else -> null
            }
        }
    }

    private fun Contact.partsOrNull(): Parts? {
        val aBody = fixtureA.bodyData ?: return null
        val bBody = fixtureB.bodyData ?: return null
        return Parts(aBody, bBody, fixtureA, fixtureB)
    }

    companion object {
        private val logger = logger<ContactHandlerSystem>()
    }
}
