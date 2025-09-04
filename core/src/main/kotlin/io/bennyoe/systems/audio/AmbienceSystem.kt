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

    private var timeSec: Float = 0f
    private var rainTailUntilSec: Float = 0f
    private val rainTailSec: Float = 12f
    private var isRainingNow: Boolean = false

    private val soundsFadingOut = mutableListOf<FadingSound>()

    private data class AmbienceSignature(
        val zoneId: String?,
        val timeOfDay: TimeOfDay,
        val weather: Weather,
    )

    private var lastSig: AmbienceSignature? = null

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
                }
                true
            }

            else -> false
        }

    override fun onTick() {
        timeSec += deltaTime
        baseSound?.update(deltaTime)
        timeSound?.update(deltaTime)
        weatherSound?.update(deltaTime)

        if (!isRainingNow && rainTailUntilSec > 0f && timeSec >= rainTailUntilSec) {
            weatherSound?.let { ws ->
                if (!ws.isStopped()) {
                    ws.fadeOut() // idempotent if already fading
                } else {
                    weatherSound = null
                }
            }
            // Reset so this block runs only once per tail
            rainTailUntilSec = 0f
        }

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

        val signature =
            AmbienceSignature(
                zoneId = currentAmbienceCmp?.type?.name,
                timeOfDay = gameStateCmp.getTimeOfDay(),
                weather = gameStateCmp.weather,
            )

        if (signature != lastSig) {
            applySignatureChange(previous = lastSig, current = signature)
            lastSig = signature
        }
    }

    private fun applySignatureChange(
        previous: AmbienceSignature?,
        current: AmbienceSignature,
    ) {
        isRainingNow = (current.weather == Weather.RAIN)
        val variations = currentAmbienceCmp?.variations
        val volume = currentAmbienceCmp?.volume ?: 1f

        // No active zone → fade out everything
        if (variations == null) {
            stopAllSources()
            return
        }

        // Base layer (zone dependent)
        val basePath = variations[SoundVariation.BASE]
        baseSound =
            updateSource(
                soundPath = basePath,
                volume = volume,
                currentSound = baseSound,
                fadeInSec = 2.0f,
                fadeOutSec = 6f,
                startDelaySec = 0f,
            )

        // Time-of-day layer
        val timeVar = if (current.timeOfDay == TimeOfDay.DAY) SoundVariation.DAY else SoundVariation.NIGHT
        val timePath = variations[timeVar]
        timeSound =
            updateSource(
                soundPath = timePath,
                volume = volume,
                currentSound = timeSound,
                fadeInSec = 2.0f,
                fadeOutSec = 6f,
                startDelaySec = 0f,
            )

        // Weather layer (tail-aware retargeting for rain)
        val weatherChanged = previous?.weather != current.weather
        val rainStopped = previous?.weather == Weather.RAIN && current.weather != Weather.RAIN

        if (rainStopped) {
            // Start tail window and let current rain instance fade out
            rainTailUntilSec = timeSec + rainTailSec
            weatherSound?.let {
                it.fadeOut()
                if (!soundsFadingOut.contains(it)) soundsFadingOut.add(it)
            }
        }

        val tailActive = timeSec < rainTailUntilSec
        val tailRemaining = (rainTailUntilSec - timeSec).coerceAtLeast(0f)

        // During tail, still target the zone's RAIN path so zone switches can retarget the rain
        val desiredWeatherPath =
            when {
                current.weather == Weather.RAIN -> variations[SoundVariation.RAIN]
                tailActive -> variations[SoundVariation.RAIN]
                else -> null
            }

        val isRealRain = current.weather == Weather.RAIN
        // if it is actually raining fade with 22sec. If it is a rain tail fade with 0.35sec
        val fadeInForWeather = if (weatherChanged) 22f else kotlin.math.min(0.35f, tailRemaining)
        val fadeOutForWeather = if (isRealRain) 12f else tailRemaining
        val startDelayForRain = if (weatherChanged && isRealRain) RAIN_DELAY else 0f

        weatherSound =
            updateSource(
                soundPath = desiredWeatherPath,
                volume = volume,
                currentSound = weatherSound,
                fadeInSec = fadeInForWeather,
                fadeOutSec = fadeOutForWeather,
                startDelaySec = startDelayForRain,
            )
    }

    private fun updateSource(
        soundPath: String?,
        volume: Float,
        currentSound: FadingSound?,
        fadeInSec: Float,
        fadeOutSec: Float,
        startDelaySec: Float,
    ): FadingSound? {
        val isPlaying = currentSound != null

        // Should play but isn't / or path changed → crossfade to new
        if (soundPath != null && (!isPlaying || currentSound.path != soundPath)) {
            currentSound?.let {
                it.fadeOut()
                soundsFadingOut.add(it)
            }
            return FadingSound(soundPath, volume, fadeInSec, fadeOutSec, reverb, startDelaySec)
        }

        // Should stop but is playing → fade out
        if (soundPath == null && isPlaying) {
            currentSound.fadeOut()
            soundsFadingOut.add(currentSound)
            return null
        }

        // Unchanged
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
