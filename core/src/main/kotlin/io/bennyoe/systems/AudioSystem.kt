package io.bennyoe.systems

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import com.github.quillraven.fleks.World.Companion.inject
import de.pottgames.tuningfork.Audio
import de.pottgames.tuningfork.BufferedSoundSource
import de.pottgames.tuningfork.StreamedSoundSource
import io.bennyoe.assets.SoundAssets
import io.bennyoe.components.AudioComponent
import io.bennyoe.components.PhysicComponent
import io.bennyoe.components.PlayerComponent
import io.bennyoe.components.TransformComponent
import io.bennyoe.event.MapChangedEvent
import io.bennyoe.event.PlayLoopingSoundEvent
import io.bennyoe.event.PlaySoundEvent
import io.bennyoe.event.StopLoopingSoundEvent
import io.bennyoe.utility.FloorType
import ktx.assets.async.AssetStorage
import ktx.log.logger
import ktx.tiled.propertyOrNull
import kotlin.reflect.KClass

class AudioSystem(
    private val assets: AssetStorage = inject("assetManager"),
    private val audio: Audio = inject("audio"),
) : IteratingSystem(family { all(AudioComponent, TransformComponent) }),
    EventListener {
    private lateinit var bgMusic: StreamedSoundSource
    private val loopingSounds = mutableMapOf<SoundTypes, BufferedSoundSource>()
    private val eventHandlers = mutableMapOf<KClass<out Event>, (Event) -> Unit>()
    private val playerEntity by lazy { world.family { all(PlayerComponent, PhysicComponent) }.first() }

    // map the floorTypes to the footsteps
    private val footstepSounds =
        mapOf(
            FloorType.STONE to SoundAssets.FOOTSTEPS_STONE,
            FloorType.WOOD to SoundAssets.FOOTSTEPS_WOOD,
            null to SoundAssets.FOOTSTEPS_STONE,
        )

    init {
        registerHandler(PlaySoundEvent::class) { event ->
            val soundBuffer = assets[event.sound.descriptor]
            soundBuffer.play(event.volume)
        }

        registerHandler(PlayLoopingSoundEvent::class) { event ->
            if (loopingSounds.containsKey(event.loopId)) return@registerHandler

            val soundAsset =
                when (event.loopId) {
                    SoundTypes.FOOTSTEPS -> footstepSounds[event.floorType] ?: footstepSounds[null]!!
                    else -> null
                }

            if (soundAsset != null) {
                val soundBuffer = assets[soundAsset.descriptor]
                val source = audio.obtainSource(soundBuffer)
                source.setLooping(true)
                source.volume = event.volume
                source.play()
                loopingSounds[event.loopId] = source
            }
        }

        registerHandler(StopLoopingSoundEvent::class) { event ->
            loopingSounds[event.loopId]?.stop()
            loopingSounds.remove(event.loopId)
        }
    }

    override fun onTick() {
        val playerPhysicCmp = playerEntity[PhysicComponent]
        val playerPos = playerPhysicCmp.body.position

        audio.listener.setPosition(playerPos.x, playerPos.y, 0f)
        super.onTick()
    }

    override fun onTickEntity(entity: Entity) {
        val soundCmp = entity[AudioComponent]
        val transformCmp = entity[TransformComponent]

        if (soundCmp.bufferedSoundSource == null) {
            val source = audio.obtainSource(assets[soundCmp.soundAsset.descriptor])
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
                    bgMusic.setLooping(true)
                    bgMusic.volume = 0.4f
                    bgMusic.play()
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

enum class SoundTypes {
    NONE,
    FOOTSTEPS,
    CAMPFIRE,
}
