package io.bennyoe.systems.audio

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import com.github.quillraven.fleks.World.Companion.inject
import de.pottgames.tuningfork.Audio
import de.pottgames.tuningfork.BufferedSoundSource
import de.pottgames.tuningfork.StreamedSoundSource
import io.bennyoe.components.PhysicComponent
import io.bennyoe.components.PlayerComponent
import io.bennyoe.components.TransformComponent
import io.bennyoe.components.audio.AudioComponent
import io.bennyoe.components.audio.SoundProfileComponent
import io.bennyoe.config.GameConstants.EFFECT_VOLUME
import io.bennyoe.event.PlayLoopingSoundEvent
import io.bennyoe.event.PlaySoundEvent
import io.bennyoe.event.StopLoopingSoundEvent
import io.bennyoe.event.StreamSoundEvent
import io.bennyoe.lightEngine.core.FaultyLightEvent
import io.bennyoe.lightEngine.core.LightEngineEvent
import io.bennyoe.lightEngine.core.LightEngineEventConsumer
import io.bennyoe.lightEngine.core.LightEngineEventListener
import io.bennyoe.lightEngine.core.LightningEvent
import ktx.assets.async.AssetStorage
import ktx.log.logger
import ktx.math.vec3
import kotlin.collections.set

private const val MIN_PITCH = 0.8f
private const val MAX_PITCH = 1.3f
private const val THUNDER_DELAY = 1f
private const val BUZZ_SOUND_LIFETIME = 0.1f

/**
 * A Fleks [IteratingSystem] responsible for managing spatialized sound effects in the game.
 *
 * This system handles playback of both one-shot and looping sound effects using TuningFork's [BufferedSoundSource]s.
 * It also ensures correct spatial positioning and reverb handling of all sound sources.
 *
 * ## Core Responsibilities:
 * - Processes entities with [AudioComponent] and [TransformComponent] to spawn and update sounds
 * - Listens to sound-related events like [PlaySoundEvent], [PlayLoopingSoundEvent], [StopLoopingSoundEvent], and [StreamSoundEvent]
 * - Applies 3D sound positioning, volume, attenuation, and optional pitch variation
 * - Registers and unregisters active sources with the [ReverbSystem] for environmental effects
 * - Maintains cleanup of finished one-shot sound sources
 *
 * ## Notes:
 * - Looping sounds are tracked by [SoundType] and only one instance per type can play at once
 * - Player position is used to update the global audio listener for spatialized sound rendering
 *
 * ## Disposal:
 * - Frees all active sources and releases audio resources on system shutdown
 *
 * @see AudioComponent for controlling playback parameters
 * @see SoundProfileComponent for profile-based sound selection
 * @see ReverbSystem for applying environmental effects
 */
class SoundEffectSystem(
    private val assets: AssetStorage = inject("assetManager"),
    private val audio: Audio = inject("audio"),
) : IteratingSystem(family { all(AudioComponent, TransformComponent) }),
    EventListener,
    LightEngineEventConsumer {
    private val loopingSounds = mutableMapOf<SoundType, BufferedSoundSource>()
    private val playerEntity by lazy { world.family { all(PlayerComponent, PhysicComponent) }.first() }
    private val oneShotSoundSources = mutableListOf<BufferedSoundSource>()
    private val reverb = world.system<ReverbSystem>()
    private var thunderTriggered = false
    private var thunderDelayCounter = 0f
    private var buzzSound = StreamedSoundSource(Gdx.files.internal("sound/faulty_lamp.mp3"))
    private val thunderPathList = listOf<String>("sound/thunder_1.mp3", "sound/thunder_2.mp3")

    private var buzzSoundTimeout = 0f

    init {
        buzzSound.isRelative = false
        buzzSound.setLooping(true)
        buzzSound.volume = .1f
        buzzSound.attenuationFactor = 3f
        buzzSound.playbackPosition = 3f
        LightEngineEventListener.subscribe(this)
    }

    override fun onEvent(event: LightEngineEvent) {
        when (event) {
            is LightningEvent -> thunderTriggered = true
            is FaultyLightEvent -> {
                if (event.lightIsOn) {
                    reverb.registerSource(buzzSound)
                    buzzSound.setPosition(vec3(event.position.x, event.position.y, 0f))
                    if (!buzzSound.isPlaying) {
                        buzzSound.play()
                    }
                    buzzSoundTimeout = BUZZ_SOUND_LIFETIME
                }
            }

            else -> Unit
        }
    }

    override fun onTick() {
        val playerPos = playerEntity[TransformComponent].position
        triggerThunder()

        if (buzzSound.isPlaying) {
            if (buzzSoundTimeout > 0f) {
                buzzSoundTimeout -= deltaTime
            } else {
                buzzSound.stop()
            }
        }

        audio.listener.setPosition(playerPos.x, playerPos.y, 0f)
        cleanUpOneShotSounds()
        super.onTick()
    }

    override fun onTickEntity(entity: Entity) {
        val soundCmp = entity[AudioComponent]
        val transformCmp = entity[TransformComponent]

        if (soundCmp.bufferedSoundSource == null) {
            with(world) {
                entity.getOrNull(SoundProfileComponent)?.profile
            }

            val soundAsset = SoundMappingService.getSoundAsset(soundCmp.soundType) ?: return
            val source = audio.obtainSource(assets[soundAsset.descriptor.random()])
            source.volume = soundCmp.soundVolume
            source.attenuationFactor = 1f
            source.attenuationMaxDistance = soundCmp.soundAttenuationMaxDistance
            source.attenuationMinDistance = soundCmp.soundAttenuationMinDistance
            source.attenuationFactor = soundCmp.soundAttenuationFactor
            reverb.registerSource(source)
            source.setLooping(soundCmp.isLooping)
            source.isRelative = false
            soundCmp.bufferedSoundSource = source
            source.play()
        }

        soundCmp.bufferedSoundSource?.setPosition(transformCmp.position.x + transformCmp.width * 0.5f, transformCmp.position.y, 0f)
    }

    private fun triggerThunder() {
        if (thunderTriggered && thunderDelayCounter < THUNDER_DELAY) {
            thunderDelayCounter += deltaTime
            return
        }

        if (thunderTriggered) {
            val triggeredSound = StreamedSoundSource(Gdx.files.internal(thunderPathList.random()))
            triggeredSound.isRelative = true
            triggeredSound.setLooping(false)
            reverb.registerSource(triggeredSound)
            triggeredSound.volume = EFFECT_VOLUME
            triggeredSound.play()
            thunderDelayCounter = 0f
            thunderTriggered = false
        }
    }

    private fun cleanUpOneShotSounds() {
        val iterator = oneShotSoundSources.iterator()
        while (iterator.hasNext()) {
            val source = iterator.next()
            if (!source.isPlaying) {
                world.system<ReverbSystem>().unregisterSource(source)
                source.free()
                iterator.remove()
            }
        }
    }

    override fun onDispose() {
        LightEngineEventListener.unsubscribe(this)
        loopingSounds.forEach { (_, source) -> source.free() }
        oneShotSoundSources.forEach { it.free() }
        oneShotSoundSources.clear()
        loopingSounds.clear()
        super.onDispose()
    }

    companion object {
        val logger = logger<SoundEffectSystem>()
    }

    override fun handle(event: Event): Boolean {
        return when (event) {
            is PlaySoundEvent -> {
                val soundProfile =
                    with(world) {
                        event.entity.getOrNull(SoundProfileComponent)?.profile
                    }

                val shouldVary = event.soundType.vary
                val soundAsset = SoundMappingService.getSoundAsset(event.soundType, soundProfile, event.floorType) ?: return true
                val soundBuffer = assets[soundAsset.descriptor.random()]
                val source = audio.obtainSource(soundBuffer)

                source.isRelative = true
                event.position?.let {
                    source.setPosition(it.x, it.y, 0f)
                    source.isRelative = false
                    source.attenuationFactor = 3f
                }

                reverb.registerSource(source)
                source.volume = event.volume * EFFECT_VOLUME
                if (shouldVary) {
                    source.pitch = MathUtils.random(MIN_PITCH, MAX_PITCH)
                }

                source.play()
                oneShotSoundSources.add(source)
                true
            }

            is PlayLoopingSoundEvent -> {
                if (loopingSounds.containsKey(event.soundType)) return true

                val soundProfile =
                    with(world) {
                        event.entity.getOrNull(SoundProfileComponent)?.profile
                    }

                val soundAsset = SoundMappingService.getSoundAsset(event.soundType, soundProfile, event.floorType) ?: return true

                val soundBuffer = assets[soundAsset.descriptor.random()]
                val source = audio.obtainSource(soundBuffer)
                source.setLooping(true)

                reverb.registerSource(source)

                source.volume = event.volume * EFFECT_VOLUME
                source.attenuationFactor = 1f
                source.play()
                loopingSounds[event.soundType] = source
                true
            }

            is StopLoopingSoundEvent -> {
                loopingSounds[event.loopId]?.stop()
                loopingSounds[event.loopId]?.free()
                loopingSounds.remove(event.loopId)
                true
            }

            is StreamSoundEvent -> {
                val triggeredSound = StreamedSoundSource(Gdx.files.internal(event.sound))
                if (event.position != null) {
                    triggeredSound.isRelative = false
                    triggeredSound.setPosition(vec3(event.position.x, event.position.y, 0f))
                } else {
                    triggeredSound.isRelative = true
                }
                triggeredSound.setLooping(event.looping)
                reverb.registerSource(triggeredSound)
                triggeredSound.volume = event.volume * EFFECT_VOLUME
                triggeredSound.play()
                true
            }

            else -> false
        }
    }
}
