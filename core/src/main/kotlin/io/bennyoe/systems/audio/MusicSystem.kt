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
import io.bennyoe.components.GameMood
import io.bennyoe.components.GameStateComponent
import io.bennyoe.event.MapChangedEvent
import ktx.tiled.propertyOrNull

/**
 * A Fleks [IntervalSystem] that manages dynamic music playback using the TuningFork JukeBox system.
 *
 * This system reacts to [MapChangedEvent]s by reading music file paths from map properties
 * and constructing streamed [Song] instances for each game mood:
 * - Background (default exploration or idle)
 * - Chase (enemy encounters)
 * - Dead (player has died)
 *
 * Each mood-specific [Song] is stored in a [PlayList], which is managed by a [ThemePlayListProvider].
 * The appropriate playlist is selected based on the current [GameMood], as determined
 * by the [GameStateComponent] on a global game state entity.
 *
 * Music playback is handled via [JukeBox], which is automatically updated on each tick.
 * When the player's mood changes, [JukeBox.softStopAndResume] is triggered to perform a smooth
 * transition to the newly selected music theme.
 *
 * ## Features:
 * - Streaming background music with looped [StreamedSoundSource]s
 * - Automatic theme switching based on [GameMood]
 * - Crossfading between music states using linear interpolation
 * - Lazy initialization of music assets and systems
 *
 * ## Disposal:
 * Disposes each streamed source individually on system shutdown.
 */
class MusicSystem :
    IntervalSystem(),
    EventListener {
    private val gameStateEntity by lazy { world.family { all(GameStateComponent) }.first() }
    private var bgMusic: StreamedSoundSource? = null
    private var chaseMusic: StreamedSoundSource? = null
    private var deadMusic: StreamedSoundSource? = null
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

    override fun handle(event: Event?): Boolean {
        val gameStateCmp = gameStateEntity[GameStateComponent]
        when (event) {
            is MapChangedEvent -> {
                event.map.propertyOrNull<String>("bgMusic")?.let { path ->
                    bgMusic = StreamedSoundSource(Gdx.files.internal(path))
                    bgMusic?.isRelative = true
                    bgMusic?.setLooping(true)
                    bgMusic?.volume = 0.4f
                    val bgSongSettings = SongSettings.linear(0.6f, 1f, 1f)
                    val meta = SongMeta().setTitle("BG-MUSIC")
                    val bgSong = Song(bgMusic, bgSongSettings, meta)
                    bgMusicPlayList.addSong(bgSong)
                }
                event.map.propertyOrNull<String>("chaseMusic")?.let { path ->
                    chaseMusic = StreamedSoundSource(Gdx.files.internal(path))
                    chaseMusic?.isRelative = true
                    chaseMusic?.setLooping(true)
                    val chaseSongSettings = SongSettings.linear(0.6f, 1f, 1f)
                    val meta = SongMeta().setTitle("CHASE-MUSIC")
                    val chaseSong = Song(chaseMusic, chaseSongSettings, meta)
                    chaseMusicPlayList.addSong(chaseSong)
                }
                event.map.propertyOrNull<String>("deadMusic")?.let { path ->
                    deadMusic = StreamedSoundSource(Gdx.files.internal(path))
                    deadMusic?.isRelative = true
                    deadMusic?.setLooping(true)
                    val deadSongSettings = SongSettings.linear(0.6f, 1f, 1f)
                    val meta = SongMeta().setTitle("DEAD-MUSIC")
                    val deadSong = Song(deadMusic, deadSongSettings, meta)
                    deadMusicPlaylist.addSong(deadSong)
                }
                if (!musicJukebox.isPlaying) {
                    musicJukebox.volume = gameStateCmp.musicVolume
                    musicJukebox.play()
                }
            }
        }
        return true
    }

    override fun onTick() {
        musicJukebox.volume = gameStateEntity[GameStateComponent].musicVolume
        musicJukebox.update()
        updateMusicTheme()
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

    override fun onDispose() {
        bgMusic?.dispose()
        chaseMusic?.dispose()
        deadMusic?.dispose()
        super.onDispose()
    }
}
