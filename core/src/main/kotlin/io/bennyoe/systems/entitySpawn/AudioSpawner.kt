package io.bennyoe.systems.entitySpawn

import com.badlogic.gdx.maps.MapLayer
import com.github.quillraven.fleks.World
import io.bennyoe.components.IsIndoorComponent
import io.bennyoe.components.PhysicComponent
import io.bennyoe.components.audio.AmbienceSoundComponent
import io.bennyoe.components.audio.AmbienceType
import io.bennyoe.components.audio.ReverbZoneComponent
import io.bennyoe.components.audio.SoundTriggerComponent
import io.bennyoe.components.audio.SoundVariation
import io.bennyoe.config.EntityCategory
import io.bennyoe.systems.audio.SoundType
import io.bennyoe.utility.EntityBodyData
import io.bennyoe.utility.SensorType
import ktx.tiled.shape

class AudioSpawner(
    val world: World,
    val phyWorld: com.badlogic.gdx.physics.box2d.World,
) {
    fun spawnSoundTriggers(soundTriggersLayer: MapLayer) {
        soundTriggersLayer.objects?.forEach { soundTriggerObj ->
            world.entity {
                val physicCmp =
                    PhysicComponent.physicsComponentFromShape2D(
                        phyWorld,
                        it,
                        soundTriggerObj.shape,
                        isSensor = true,
                        sensorType = SensorType.SOUND_TRIGGER_SENSOR,
                        setUserData = EntityBodyData(it, EntityCategory.SENSOR),
                        categoryBit = EntityCategory.SENSOR.bit,
                    )
                it += physicCmp
                it +=
                    SoundTriggerComponent(
                        soundTriggerObj.properties.get("sound") as String?,
                        (soundTriggerObj.properties.get("type") as String?)?.uppercase()?.let { type -> SoundType.valueOf(type) },
                        soundTriggerObj.properties.get("streamed", Boolean::class.java),
                        soundTriggerObj.properties.get("volume", Float::class.java),
                    )
            }
        }
    }

    fun spawnReverbZones(reverbZonesLayer: MapLayer) {
        reverbZonesLayer.objects.forEach { audioZoneObj ->
            world.entity {
                val physicCmp =
                    PhysicComponent.physicsComponentFromShape2D(
                        phyWorld,
                        it,
                        audioZoneObj.shape,
                        isSensor = true,
                        sensorType = SensorType.AUDIO_EFFECT_SENSOR,
                        setUserData = EntityBodyData(it, EntityCategory.SENSOR),
                        categoryBit = EntityCategory.SENSOR.bit,
                    )
                it += physicCmp
                it +=
                    ReverbZoneComponent(
                        audioZoneObj.properties.get("effectPreset", String::class.java),
                        audioZoneObj.properties.get("effectIntensity", Float::class.java),
                    )
            }
        }
    }

    fun spawnAmbienceZones(ambienceZonesLayer: MapLayer) {
        ambienceZonesLayer.objects?.forEach { ambienceSoundObj ->
            world.entity { entity ->
                val physicCmp =
                    PhysicComponent.physicsComponentFromShape2D(
                        phyWorld,
                        entity,
                        ambienceSoundObj.shape,
                        isSensor = true,
                        sensorType = SensorType.SOUND_AMBIENCE_SENSOR,
                        setUserData = EntityBodyData(entity, EntityCategory.SENSOR),
                        categoryBit = EntityCategory.SENSOR.bit,
                    )
                entity += physicCmp

                entity +=
                    IsIndoorComponent(
                        ambienceSoundObj.properties.get("isIndoor") as? Boolean ?: false,
                    )

                val baseSound = ambienceSoundObj.properties.get("base")?.let { SoundVariation.BASE to it as String }
                val daySound = ambienceSoundObj.properties.get("day")?.let { SoundVariation.DAY to it as String }
                val nightSound = ambienceSoundObj.properties.get("night")?.let { SoundVariation.NIGHT to it as String }
                val rainSound = ambienceSoundObj.properties.get("rain")?.let { SoundVariation.RAIN to it as String }
                val variations = listOfNotNull(baseSound, daySound, nightSound, rainSound).toMap()

                entity +=
                    AmbienceSoundComponent(
                        AmbienceType.valueOf((ambienceSoundObj.properties.get("type") as String).uppercase()),
                        variations,
                        ambienceSoundObj.properties.get("volume") as? Float,
                    )
            }
        }
    }
}
