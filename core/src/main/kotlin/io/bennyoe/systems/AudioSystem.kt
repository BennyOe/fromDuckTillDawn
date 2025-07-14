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
import io.bennyoe.event.AttackSoundEvent
import io.bennyoe.event.HitSoundEvent
import io.bennyoe.event.MapChangedEvent
import io.bennyoe.event.WalkSoundEvent
import io.bennyoe.event.WalkSoundStopEvent
import ktx.assets.async.AssetStorage
import ktx.log.logger
import ktx.tiled.propertyOrNull

class AudioSystem(
    private val assets: AssetStorage = inject("assetManager"),
    private val audio: Audio = inject("audio"),
) : IntervalSystem(),
    EventListener {
    private lateinit var bgMusic: StreamedSoundSource
    private val loopingSounds = mutableMapOf<String, BufferedSoundSource>()

    override fun onTick() {
    }

    override fun handle(event: Event): Boolean {
        when (event) {
            is MapChangedEvent -> {
                event.map.propertyOrNull<String>("bgMusic")?.let { path ->
                    logger.debug { "Music $path Played" }
                    bgMusic = StreamedSoundSource(Gdx.files.internal(path))
                    bgMusic.setLooping(true)
                    bgMusic.volume = 0.4f
                    bgMusic.play()
                }
                return true
            }

            is AttackSoundEvent -> {
                val sound = assets[SoundAssets.ATTACK_SOUND.descriptor]
                sound.play()
                return true
            }

            is WalkSoundEvent -> {
                if (loopingSounds.containsKey(event.soundFile)) return true // Prevent playing the same loop multiple times
                val sound = assets[SoundAssets.WALK_SOUND.descriptor]
                val soundSource = audio.obtainSource(sound)
                loopingSounds[event.soundFile] = soundSource
                soundSource.play()
            }

            is WalkSoundStopEvent -> {
                val soundSource = loopingSounds.remove(event.soundFile)
                soundSource?.stop()
                soundSource?.free()
            }

            is HitSoundEvent -> {
                val sound = assets[SoundAssets.HIT_SOUND.descriptor]
                sound.play()
            }
        }
        return false
    }

    override fun onDispose() {
        loopingSounds.clear()
        bgMusic.dispose()
        audio.dispose()
        super.onDispose()
    }

    companion object {
        val logger = logger<AudioSystem>()
    }
}
