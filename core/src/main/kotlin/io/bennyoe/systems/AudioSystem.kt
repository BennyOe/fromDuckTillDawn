package io.bennyoe.systems

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.github.quillraven.fleks.IntervalSystem
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
) : IntervalSystem(),
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
