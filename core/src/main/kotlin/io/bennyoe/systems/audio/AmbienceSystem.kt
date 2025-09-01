package io.bennyoe.systems.audio

import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.github.quillraven.fleks.IntervalSystem
import io.bennyoe.components.GameStateComponent
import io.bennyoe.components.TimeOfDay
import io.bennyoe.components.Weather
import io.bennyoe.components.audio.AmbienceSoundComponent
import io.bennyoe.components.audio.SoundVariation
import io.bennyoe.config.GameConstants.RAIN_DELAY
import io.bennyoe.event.AmbienceChangeEvent
import io.bennyoe.event.MapChangedEvent
import ktx.log.logger

class AmbienceSystem :
    IntervalSystem(),
    EventListener {
    private val gameStateCmp by lazy { world.family { all(GameStateComponent) }.first()[GameStateComponent] }
    private val reverb by lazy { world.system<ReverbSystem>() }
    private var currentAmbienceCmp: AmbienceSoundComponent? = null

    private var baseSound: FadingSound? = null
    private var timeSound: FadingSound? = null
    private var weatherSound: FadingSound? = null

    private val soundsFadingOut = mutableListOf<FadingSound>()

    override fun handle(event: Event): Boolean =
        when (event) {
            is MapChangedEvent -> {
                stopAllSources()
                currentAmbienceCmp = null
                false
            }

            is AmbienceChangeEvent -> {
                val newAmbienceCmp =
                    world
                        .family { all(AmbienceSoundComponent) }
                        .firstOrNull { it[AmbienceSoundComponent].type == event.type }
                        ?.get(AmbienceSoundComponent)

                // Only change if we entered a new, different zone
                if (newAmbienceCmp != currentAmbienceCmp) {
                    logger.debug { "Ambience changed to ${newAmbienceCmp?.type}" }
                    currentAmbienceCmp = newAmbienceCmp
                    stopAllSources()
                    updateAllLayers()
                }
                true
            }

            else -> false
        }

    override fun onTick() {
        baseSound?.update(deltaTime)
        timeSound?.update(deltaTime)
        weatherSound?.update(deltaTime)

        // Update and clean up sounds that are fading out.
        soundsFadingOut.iterator().let { iterator ->
            while (iterator.hasNext()) {
                val sound = iterator.next()
                sound.update(deltaTime)
                if (sound.isStopped()) {
                    iterator.remove()
                }
            }
        }

        updateAllLayers()
    }

    private fun updateAllLayers() {
        val variations = currentAmbienceCmp?.variations
        val volume = currentAmbienceCmp?.volume ?: 1f

        // If there's no active zone, fade out everything
        if (variations == null) {
            stopAllSources()
            return
        }

        // Layer 1: Base Sound (always plays if defined)
        baseSound = updateSourceForLayer(variations[SoundVariation.BASE], volume, baseSound)

        // Layer 2: Time of Day Sound
        val timeVariation = if (gameStateCmp.getTimeOfDay() == TimeOfDay.DAY) SoundVariation.DAY else SoundVariation.NIGHT
        timeSound = updateSourceForLayer(variations[timeVariation], volume, timeSound)

        // Layer 3: Weather Sound
        val weatherVariation = if (gameStateCmp.weather == Weather.RAIN) SoundVariation.RAIN else null
        weatherSound = updateSourceForLayer(weatherVariation?.let { variations[it] }, volume, weatherSound, SoundVariation.RAIN)
    }

    private fun updateSourceForLayer(
        soundPath: String?,
        volume: Float,
        currentSound: FadingSound?,
        type: SoundVariation? = null,
    ): FadingSound? {
        val soundIsPlaying = currentSound != null

        // Case 1: Sound should play, but isn't.
        if (soundPath != null && (!soundIsPlaying || currentSound.path != soundPath)) {
            currentSound?.fadeOut()
            currentSound?.let {
                it.fadeOut()
                soundsFadingOut.add(it)
            }
            return if (type == SoundVariation.RAIN) {
                FadingSound(soundPath, volume, RAIN_DELAY, reverb, RAIN_DELAY) // Start new rain sound
            } else {
                FadingSound(soundPath, volume, 2.0f, reverb) // Start new sound
            }
        }

        // Case 2: Sound should stop, but is playing.
        if (soundPath == null && soundIsPlaying) {
            currentSound.fadeOut()
            currentSound.let {
                it.fadeOut()
                soundsFadingOut.add(it)
            }
            return null // Sound will be removed once fade-out is complete
        }

        // Case 3: Sound is playing and should continue, or should not play and isn't.
        return currentSound
    }

    private fun stopAllSources() {
        baseSound?.let {
            it.fadeOut()
            soundsFadingOut.add(it)
        }
        timeSound?.let {
            it.fadeOut()
            soundsFadingOut.add(it)
        }
        weatherSound?.let {
            it.fadeOut()
            soundsFadingOut.add(it)
        }
        baseSound = null
        timeSound = null
        weatherSound = null
    }

    override fun onDispose() {
        // Ensure sources are stopped without fading
        baseSound?.stop()
        timeSound?.stop()
        weatherSound?.stop()
        super.onDispose()
    }

    companion object {
        val logger = logger<AmbienceSystem>()
    }
}
