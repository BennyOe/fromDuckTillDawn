package io.bennyoe.systems

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
import de.pottgames.tuningfork.EaxReverb
import de.pottgames.tuningfork.SoundEffect
import de.pottgames.tuningfork.StreamedSoundSource
import io.bennyoe.components.AudioComponent
import io.bennyoe.components.PhysicComponent
import io.bennyoe.components.PlayerComponent
import io.bennyoe.components.SoundProfileComponent
import io.bennyoe.components.TransformComponent
import io.bennyoe.event.MapChangedEvent
import io.bennyoe.event.PlayLoopingSoundEvent
import io.bennyoe.event.PlaySoundEvent
import io.bennyoe.event.PlayerEnteredAudioZoneEvent
import io.bennyoe.event.StopLoopingSoundEvent
import io.bennyoe.service.SoundMappingService
import io.bennyoe.service.SoundType
import ktx.assets.async.AssetStorage
import ktx.log.logger
import ktx.math.plus
import ktx.tiled.propertyOrNull
import kotlin.reflect.KClass

private const val MIN_PITCH = 0.8f
private const val MAX_PITCH = 1.3f

/**
 * Manages all audio playback within the game, including sound effects and background music.
 *
 * This system centralizes audio handling through a clear, event-driven, and component-based flow:
 * 1.  **Trigger**: Sounds are requested either by firing a [PlaySoundEvent]/[PlayLoopingSoundEvent] or by an entity
 * with an [AudioComponent]. Both methods use a logical [SoundType] enum to specify the sound's purpose
 * (e.g., `FOOTSTEPS`, `ATTACK`).
 * 2.  **Mapping**: The system uses the [SoundMapping] service to translate the logical [SoundType] into a
 * concrete [io.bennyoe.assets.SoundAssets] file. This mapping can be context-dependent, for example,
 * using the [io.bennyoe.utility.FloorType] to select the correct footstep sound.
 * 3.  **Asset Loading**: The resolved asset descriptor is used to fetch the loaded [de.pottgames.tuningfork.SoundBuffer]
 * from the [AssetStorage].
 * 4.  **Playback**: Finally, the TuningFork [Audio] engine is used to obtain a source and play the sound.
 * The system handles positioning by updating the listener's position to the player's location
 * and setting the source's position for spatial audio effects.
 *
 * Looping sounds are managed in the `loopingSounds` map to ensure they can be started and stopped correctly.
 */
class AudioSystem(
    private val assets: AssetStorage = inject("assetManager"),
    private val audio: Audio = inject("audio"),
) : IteratingSystem(family { all(AudioComponent, TransformComponent) }),
    EventListener {
    private lateinit var bgMusic: StreamedSoundSource
    private val loopingSounds = mutableMapOf<SoundType, BufferedSoundSource>()
    private val eventHandlers = mutableMapOf<KClass<out Event>, (Event) -> Unit>()
    private val playerEntity by lazy { world.family { all(PlayerComponent, PhysicComponent) }.first() }

    init {
        val soundEffect = SoundEffect(EaxReverb.arena())
        registerHandler(PlaySoundEvent::class) { event ->
            val soundProfile =
                with(world) {
                    event.entity.getOrNull(SoundProfileComponent)?.profile
                }

            val shouldVary = event.soundType.vary
            val soundAsset = SoundMappingService.getSoundAsset(event.soundType, soundProfile, event.floorType) ?: return@registerHandler
            val soundBuffer = assets[soundAsset.descriptor]
            val source = audio.obtainSource(soundBuffer)

            source.isRelative = true
            event.position?.let {
                source.setPosition(it.x, it.y, 0f)
                source.isRelative = false
                source.attenuationFactor = 3f
            }
            source.volume = event.volume
            if (shouldVary) {
                source.pitch = MathUtils.random(MIN_PITCH, MAX_PITCH)
            }
//            source.setFilter(0f, 0f)
//            source.attachEffect(soundEffect)
            source.play()
        }

        registerHandler(PlayLoopingSoundEvent::class) { event ->
            if (loopingSounds.containsKey(event.soundType)) return@registerHandler

            val soundProfile =
                with(world) {
                    event.entity.getOrNull(SoundProfileComponent)?.profile
                }

            val soundAsset = SoundMappingService.getSoundAsset(event.soundType, soundProfile, event.floorType) ?: return@registerHandler

            val soundBuffer = assets[soundAsset.descriptor]
            val source = audio.obtainSource(soundBuffer)
            source.setLooping(true)
            source.volume = event.volume
            source.attenuationFactor = 1f
            source.play()
            loopingSounds[event.soundType] = source
        }

        registerHandler(StopLoopingSoundEvent::class) { event ->
            loopingSounds[event.loopId]?.stop()
            loopingSounds.remove(event.loopId)
        }

        registerHandler(PlayerEnteredAudioZoneEvent::class) { event ->
        }
    }

    override fun onTick() {
        val playerPos = playerEntity[TransformComponent].position

        audio.listener.setPosition(playerPos.x, playerPos.y, 0f)
        super.onTick()
    }

    override fun onTickEntity(entity: Entity) {
        val soundCmp = entity[AudioComponent]
        val transformCmp = entity[TransformComponent]

        if (soundCmp.bufferedSoundSource == null) {
            val soundProfile =
                with(world) {
                    entity.getOrNull(SoundProfileComponent)?.profile
                }

            val soundAsset = SoundMappingService.getSoundAsset(soundCmp.soundType) ?: return
            val source = audio.obtainSource(assets[soundAsset.descriptor])
            source.volume = soundCmp.soundVolume
            source.attenuationFactor = 1f
            source.attenuationMaxDistance = soundCmp.soundAttenuationMaxDistance
            source.attenuationMinDistance = soundCmp.soundAttenuationMinDistance
            source.attenuationFactor = soundCmp.soundAttenuationFactor
            source.setLooping(soundCmp.isLooping)
            source.isRelative = false
            soundCmp.bufferedSoundSource = source
            source.play()
        }

        soundCmp.bufferedSoundSource?.setPosition(transformCmp.position.x + transformCmp.width * 0.5f, transformCmp.position.y, 0f)
    }

    override fun handle(event: Event): Boolean {
        eventHandlers[event::class]?.invoke(event)

        when (event) {
            is MapChangedEvent ->
                event.map.propertyOrNull<String>("bgMusic")?.let { path ->
                    logger.debug { "Music $path Played" }
                    bgMusic = StreamedSoundSource(Gdx.files.internal(path))
                    bgMusic.isRelative = true
                    bgMusic.setLooping(true)
                    bgMusic.volume = 0.4f
//                    bgMusic.play()
                }
        }
        return true
    }

    override fun onDispose() {
        loopingSounds.clear()
        bgMusic.dispose()
        audio.dispose()
        super.onDispose()
    }

    /** The `registerHandler` function registers an event handler for a specific event type in the `eventHandlers` map. It allows the system to
     associate custom logic with different event classes, enabling dynamic event handling within the ECS framework.
     **/
    @Suppress("UNCHECKED_CAST")
    private fun <T : Event> registerHandler(
        eventClass: KClass<T>,
        handler: (T) -> Unit,
    ) {
        eventHandlers[eventClass] = { event -> handler(event as T) }
    }

    companion object {
        val logger = logger<AudioSystem>()
    }
}
