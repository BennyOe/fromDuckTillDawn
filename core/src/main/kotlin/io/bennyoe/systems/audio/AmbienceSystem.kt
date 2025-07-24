package io.bennyoe.systems.audio

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.github.quillraven.fleks.IntervalSystem
import de.pottgames.tuningfork.StreamedSoundSource
import de.pottgames.tuningfork.jukebox.JukeBox
import de.pottgames.tuningfork.jukebox.playlist.PlayList
import de.pottgames.tuningfork.jukebox.playlist.ThemePlayListProvider
import de.pottgames.tuningfork.jukebox.song.Song
import de.pottgames.tuningfork.jukebox.song.SongMeta
import de.pottgames.tuningfork.jukebox.song.SongSettings
import io.bennyoe.components.audio.AmbienceSoundComponent
import io.bennyoe.event.AmbienceChangeEvent
import io.bennyoe.event.MapChangedEvent

/**
 * A Fleks [IntervalSystem] that manages ambient background sounds depending on the game environment.
 *
 * This system reacts to two event types:
 * - [MapChangedEvent]: Clears current ambience and rebuilds playlists for the new map.
 * - [AmbienceChangeEvent]: Changes the ambience theme and smoothly transitions to the new ambience.
 *
 * Ambience sounds are streamed and played in a loop using the TuningFork [JukeBox] and [PlayList] system.
 * Each ambience source is assigned to a [PlayList] based on its [AmbienceType] and played accordingly.
 *
 * ## Features:
 * - Soft crossfade between ambient themes using [JukeBox.softStopAndResume].
 * - Automatic recreation of ambience on map change.
 * - Support for multiple ambience tracks via [AmbienceSoundComponent].
 *
 * ## Disposal:
 * Disposes all ambience playlists and stops playback when the system is disposed.
 *
 * @see AmbienceType for available ambience themes.
 * @see AmbienceSoundComponent for entity-based ambience definitions.
 */
class AmbienceSystem :
    IntervalSystem(),
    EventListener {
    private val ambiencePlayLists: MutableList<PlayList> = mutableListOf()
    private val ambiencePlayListProvider: ThemePlayListProvider by lazy {
        ThemePlayListProvider()
    }
    private val ambienceJukebox: JukeBox by lazy { JukeBox(ambiencePlayListProvider) }
    private var currentAmbienceId: AmbienceType? = null

    override fun handle(event: Event): Boolean =
        when (event) {
            is MapChangedEvent -> {
                ambienceJukebox.stop()
                currentAmbienceId = null
                ambiencePlayLists.clear()
                createAmbiencePlaylists()
                false
            }

            is AmbienceChangeEvent -> {
                if (currentAmbienceId == event.type) true

                currentAmbienceId = event.type
                ambiencePlayListProvider.theme = event.type.ordinal
                ambienceJukebox.softStopAndResume(Interpolation.sine, 0.8f)
                true
            }

            else -> false
        }

    override fun onTick() {
        ambienceJukebox.update()
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

    override fun onDispose() {
        ambiencePlayLists.clear()
        super.onDispose()
    }

    // For unit testing: returns the number of ambience playlists
    @Suppress("ktlint:standard:function-naming")
    fun test_getPlaylistCount(): Int = ambiencePlayLists.size
}

enum class AmbienceType {
    FOREST,
    CAVE,
}
