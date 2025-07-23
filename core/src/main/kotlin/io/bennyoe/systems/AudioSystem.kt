package io.bennyoe.systems

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import com.github.quillraven.fleks.World.Companion.inject
import de.pottgames.tuningfork.Audio
import de.pottgames.tuningfork.BufferedSoundSource
import de.pottgames.tuningfork.SoundEffect
import de.pottgames.tuningfork.SoundSource
import de.pottgames.tuningfork.StreamedSoundSource
import de.pottgames.tuningfork.jukebox.JukeBox
import de.pottgames.tuningfork.jukebox.playlist.PlayList
import de.pottgames.tuningfork.jukebox.playlist.ThemePlayListProvider
import de.pottgames.tuningfork.jukebox.song.Song
import de.pottgames.tuningfork.jukebox.song.SongMeta
import de.pottgames.tuningfork.jukebox.song.SongSettings
import io.bennyoe.components.GameMood
import io.bennyoe.components.GameStateComponent
import io.bennyoe.components.PhysicComponent
import io.bennyoe.components.PlayerComponent
import io.bennyoe.components.TransformComponent
import io.bennyoe.components.audio.AmbienceSoundComponent
import io.bennyoe.components.audio.AudioComponent
import io.bennyoe.components.audio.ReverbZoneContactComponent
import io.bennyoe.components.audio.SoundProfileComponent
import io.bennyoe.event.AmbienceChangeEvent
import io.bennyoe.event.MapChangedEvent
import io.bennyoe.event.PlayLoopingSoundEvent
import io.bennyoe.event.PlaySoundEvent
import io.bennyoe.event.StopLoopingSoundEvent
import io.bennyoe.event.StreamSoundEvent
import io.bennyoe.service.SoundMappingService
import io.bennyoe.service.SoundType
import io.bennyoe.utility.getReverb
import ktx.assets.async.AssetStorage
import ktx.log.logger
import ktx.math.vec3
import ktx.tiled.propertyOrNull
import kotlin.reflect.KClass

private const val MIN_PITCH = 0.8f
private const val MAX_PITCH = 1.3f
private const val REVERB_TAIL = 5f

/**
 * Manages all audio playback within the game, including sound effects, ambience, and background music.
 *
 * This system centralizes audio handling through an event-driven and component-based architecture:
 *
 * 1. **Trigger**: Sounds are requested by firing events (e.g. [PlaySoundEvent], [PlayLoopingSoundEvent]) or by adding an [AudioComponent] to an entity.
 * 2. **Mapping**: Logical [SoundType]s are resolved via [SoundMappingService] into actual audio files, optionally influenced by [SoundProfileComponent] or floor types.
 * 3. **Asset Loading**: The resolved sound asset is loaded via [AssetStorage] and passed to TuningFork's [Audio] engine.
 * 4. **Playback**: The sound is played, optionally spatialized if a world position is given. Looping sounds are tracked for later removal.
 *
 * Additional Features:
 * - **Ambience and Music**: Background and ambience music are playlist-managed via [JukeBox] and automatically change depending on [GameMood] and map properties.
 * - **Reverb Zones**: Environmental effects (e.g., cave echo) are automatically applied when the player enters a reverb zone and gracefully fade out when leaving.
 */
class AudioSystem(
    private val assets: AssetStorage = inject("assetManager"),
    private val audio: Audio = inject("audio"),
) : IteratingSystem(family { all(AudioComponent, TransformComponent) }),
    EventListener {
    private val loopingSounds = mutableMapOf<SoundType, BufferedSoundSource>()
    private val eventHandlers = mutableMapOf<KClass<out Event>, (Event) -> Unit>()
    private val playerEntity by lazy { world.family { all(PlayerComponent, PhysicComponent) }.first() }
    private val oneShotSoundSources = mutableListOf<BufferedSoundSource>()

    // music
    private val gameStateEntity by lazy { world.family { all(GameStateComponent) }.first() }
    private lateinit var bgMusic: StreamedSoundSource
    private lateinit var chaseMusic: StreamedSoundSource
    private lateinit var deadMusic: StreamedSoundSource
    private val bgMusicPlayList: PlayList = PlayList()
    private val chaseMusicPlayList: PlayList = PlayList()
    private val deadMusicPlaylist: PlayList = PlayList()
    private val musicPlayListProvider: ThemePlayListProvider by lazy {
        ThemePlayListProvider()
            .add(chaseMusicPlayList, GameMood.CHASE.ordinal)
            .add(bgMusicPlayList, GameMood.NORMAL.ordinal)
            .add(deadMusicPlaylist, GameMood.PLAYER_DEAD.ordinal)
    }
    private val musicJukebox: JukeBox by lazy { JukeBox(musicPlayListProvider) }

    // ambience
    private val ambiencePlayLists: MutableList<PlayList> = mutableListOf()
    private val ambiencePlayListProvider: ThemePlayListProvider by lazy {
        ThemePlayListProvider()
    }
    private val ambienceJukebox: JukeBox by lazy { JukeBox(ambiencePlayListProvider) }
    private var currentAmbienceId: AmbienceType? = null

    // effects
    private var attachEffectToNewSources = false
    private var activeEffect: SoundEffect? = null
    private var activePreset: String? = null
    private var currentWet = 1f

    private data class TailReverb(
        val effect: SoundEffect,
        var ttl: Float = REVERB_TAIL,
    )

    private val detachedEffects: MutableList<TailReverb> = mutableListOf()

    init {
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

            applyReverbToNewSource(source)
            source.volume = event.volume
            if (shouldVary) {
                source.pitch = MathUtils.random(MIN_PITCH, MAX_PITCH)
            }

            source.play()
            oneShotSoundSources.add(source)
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

            applyReverbToNewSource(source)

            source.volume = event.volume
            source.attenuationFactor = 1f
            source.play()
            loopingSounds[event.soundType] = source
        }

        registerHandler(StopLoopingSoundEvent::class) { event ->
            loopingSounds[event.loopId]?.stop()
            loopingSounds[event.loopId]?.free()
            loopingSounds.remove(event.loopId)
        }

        registerHandler(StreamSoundEvent::class) { event ->
            val triggeredSound = StreamedSoundSource(Gdx.files.internal(event.sound))
            if (event.position != null) {
                triggeredSound.isRelative = false
                triggeredSound.setPosition(vec3(event.position.x, event.position.y, 0f))
            } else {
                triggeredSound.isRelative = true
            }
            triggeredSound.setLooping(false)
            applyReverbToNewSource(triggeredSound)
            triggeredSound.volume = event.volume
            triggeredSound.play()
        }

        registerHandler(AmbienceChangeEvent::class) { event ->
            if (currentAmbienceId == event.type) return@registerHandler

            currentAmbienceId = event.type
            ambiencePlayListProvider.theme = event.type.ordinal
            ambienceJukebox.softStopAndResume(Interpolation.sine, 0.8f)
        }
    }

    override fun onTick() {
        val playerPos = playerEntity[TransformComponent].position

        musicJukebox.update()
        ambienceJukebox.update()
        audio.listener.setPosition(playerPos.x, playerPos.y, 0f)

        updateMusicTheme()
        cleanUpOneShotSounds()
        updateReverbZones()

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
            val source = audio.obtainSource(assets[soundAsset.descriptor])
            source.volume = soundCmp.soundVolume
            source.attenuationFactor = 1f
            source.attenuationMaxDistance = soundCmp.soundAttenuationMaxDistance
            source.attenuationMinDistance = soundCmp.soundAttenuationMinDistance
            source.attenuationFactor = soundCmp.soundAttenuationFactor
            applyReverbToNewSource(source)
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
            is MapChangedEvent -> {
                ambienceJukebox.stop()
                currentAmbienceId = null
                ambiencePlayLists.clear()
                createAmbiencePlaylists()

                event.map.propertyOrNull<String>("bgMusic")?.let { path ->
                    bgMusic = StreamedSoundSource(Gdx.files.internal(path))
                    bgMusic.isRelative = true
                    bgMusic.setLooping(true)
                    bgMusic.volume = 0.4f
                    val bgSongSettings = SongSettings.linear(0.6f, 1f, 1f)
                    val meta = SongMeta().setTitle("BG-MUSIC")
                    val bgSong = Song(bgMusic, bgSongSettings, meta)
                    bgMusicPlayList.addSong(bgSong)
                }
                event.map.propertyOrNull<String>("chaseMusic")?.let { path ->
                    chaseMusic = StreamedSoundSource(Gdx.files.internal(path))
                    chaseMusic.isRelative = true
                    chaseMusic.setLooping(true)
                    val chaseSongSettings = SongSettings.linear(0.6f, 1f, 1f)
                    val meta = SongMeta().setTitle("CHASE-MUSIC")
                    val chaseSong = Song(chaseMusic, chaseSongSettings, meta)
                    chaseMusicPlayList.addSong(chaseSong)
                }
                event.map.propertyOrNull<String>("deadMusic")?.let { path ->
                    deadMusic = StreamedSoundSource(Gdx.files.internal(path))
                    deadMusic.isRelative = true
                    deadMusic.setLooping(true)
                    val deadSongSettings = SongSettings.linear(0.6f, 1f, 1f)
                    val meta = SongMeta().setTitle("DEAD-MUSIC")
                    val deadSong = Song(deadMusic, deadSongSettings, meta)
                    deadMusicPlaylist.addSong(deadSong)
                }
                if (!musicJukebox.isPlaying) {
                    musicJukebox.volume = 0.3f
                    musicJukebox.play()
                }
            }
        }
        return true
    }

    private fun createAmbiencePlaylists() {
        world.family { all(AmbienceSoundComponent) }.forEach { entity ->
            val ambience = entity[AmbienceSoundComponent]
            val source =
                StreamedSoundSource(Gdx.files.internal(ambience.sound)).apply {
                    isRelative = true
                    setLooping(true)
                    volume = ambience.volume!!
                }

            val settings = SongSettings.linear(1f, 2f, 2f)
            val meta = SongMeta().setTitle("Ambience-${ambience.type}")
            val song = Song(source, settings, meta)
            val playlist = PlayList()
            playlist.addSong(song)

            ambiencePlayLists.add(playlist)
            ambiencePlayListProvider.add(playlist, ambience.type.ordinal)
        }
    }

    private fun updateMusicTheme() {
        val gameStateCmp = gameStateEntity[GameStateComponent]
        val newTheme =
            when (gameStateCmp.gameMood) {
                GameMood.CHASE -> GameMood.CHASE.ordinal
                GameMood.PLAYER_DEAD -> GameMood.PLAYER_DEAD.ordinal
                else -> GameMood.NORMAL.ordinal
            }

        if (musicPlayListProvider.theme != newTheme) {
            musicPlayListProvider.theme = newTheme
            musicJukebox.softStopAndResume(Interpolation.linear, 1f)
        }
    }

    private fun updateReverbZones() {
        val currentZone = playerEntity[ReverbZoneContactComponent].activeZone
        if (currentZone != null) {
            // Change only if the preset has changed
            if (activePreset != currentZone.presetName) {
                // alten Effekt in Tail schicken
                activeEffect?.let {
                    detachEffectFromAllSources(it)
                    detachedEffects += TailReverb(it)
                }

                // create and apply new effect
                getReverb(currentZone.presetName)?.let { newFx ->
                    activeEffect = newFx
                    activePreset = currentZone.presetName
                    currentWet = currentZone.intensity.coerceIn(0f, 1f)
                    attachEffectToNewSources = true
                    attachEffectToAllSources(newFx, currentWet)
                }
            } else {
                // same preset. Only update dry/wet
                val newWet = currentZone.intensity.coerceIn(0f, 1f)
                if (!MathUtils.isEqual(newWet, currentWet)) {
                    currentWet = newWet
                    val dry = 1f - newWet
                    (oneShotSoundSources + loopingSounds.values).forEach { it.setFilter(dry, dry) }
                    world.family { all(AudioComponent) }.forEach { e ->
                        e[AudioComponent].bufferedSoundSource?.setFilter(dry, dry)
                    }
                }
            }
        } else if (activeEffect != null) {
            // player left all reverb zones
            detachEffectFromAllSources(activeEffect!!)
            detachedEffects += TailReverb(activeEffect!!)
            activeEffect = null
            activePreset = null
            attachEffectToNewSources = false
            currentWet = 1f
        }

        cleanupReverbs()
    }

    private fun attachEffectToAllSources(
        effect: SoundEffect,
        wet: Float,
    ) {
        val dry = 1f - wet
        oneShotSoundSources.forEach {
            it.setFilter(dry, dry)
            it.attachEffect(effect, wet, wet)
        }
        loopingSounds.values.forEach {
            it.setFilter(dry, dry)
            it.attachEffect(effect, wet, wet)
        }
        world.family { all(AudioComponent) }.forEach { e ->
            e[AudioComponent].bufferedSoundSource?.attachEffect(effect, wet, wet)
            e[AudioComponent].bufferedSoundSource?.setFilter(dry, dry)
        }
    }

    private fun applyReverbToNewSource(src: SoundSource) {
        if (!attachEffectToNewSources || activeEffect == null) return
        val dry = 1f - currentWet
        src.setFilter(dry, dry)
        src.attachEffect(activeEffect!!, currentWet, currentWet)
    }

    private fun cleanupReverbs() {
        val it = detachedEffects.iterator()
        while (it.hasNext()) {
            val tail = it.next()
            tail.ttl -= deltaTime
            if (tail.ttl <= 0f) {
                detachAndDisposeEffect(tail.effect)
                it.remove()
            }
        }
    }

    private fun detachAndDisposeEffect(effect: SoundEffect) {
        detachEffectFromAllSources(effect)
        effect.dispose()
    }

    private fun detachEffectFromAllSources(effect: SoundEffect) {
        oneShotSoundSources.forEach { it.detachEffect(effect) }
        loopingSounds.values.forEach { it.detachEffect(effect) }
        world.family { all(AudioComponent) }.forEach { e ->
            e[AudioComponent].bufferedSoundSource?.detachEffect(effect)
        }
    }

    private fun cleanUpOneShotSounds() {
        val iterator = oneShotSoundSources.iterator()
        while (iterator.hasNext()) {
            val source = iterator.next()
            if (!source.isPlaying) {
                source.free()
                iterator.remove()
            }
        }
    }

    override fun onDispose() {
        loopingSounds.forEach { (_, source) -> source.free() }
        oneShotSoundSources.forEach { it.free() }
        oneShotSoundSources.clear()
        loopingSounds.clear()

        bgMusic.dispose()
        chaseMusic.dispose()
        deadMusic.dispose()

        ambienceJukebox.stop()
        ambiencePlayLists.clear()

        activeEffect?.dispose()
        detachedEffects.forEach { it.effect.dispose() }
        detachedEffects.clear()

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

enum class AmbienceType {
    FOREST,
    CAVE,
}
