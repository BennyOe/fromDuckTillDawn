package io.bennyoe.systems.audio

import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.github.quillraven.fleks.IntervalSystem
import io.bennyoe.components.GameStateComponent
import io.bennyoe.components.TimeOfDay
import io.bennyoe.components.Weather
import io.bennyoe.components.audio.AmbienceSoundComponent
import io.bennyoe.components.audio.AmbienceType
import io.bennyoe.components.audio.SoundVariation
import io.bennyoe.config.GameConstants.RAIN_DELAY
import io.bennyoe.event.AmbienceChangeEvent
import io.bennyoe.event.MapChangedEvent
import ktx.log.logger

const val FADE_IN_TIME = 1.5f
const val FADE_OUT_TIME = 4f
const val RAIN_XFADE_TIME = 2f

class AmbienceSystem :
    IntervalSystem(),
    EventListener {
    private val gameStateCmp by lazy { world.family { all(GameStateComponent) }.first()[GameStateComponent] }
    private val reverb by lazy { world.system<ReverbSystem>() }

    // Sounds for the three layers
    private var activeBaseSound: FadingSound? = null
    private var fadingOutBaseSound: FadingSound? = null

    private var activeTimeSound: FadingSound? = null
    private var fadingOutTimeSound: FadingSound? = null

    // Weather sound does not need crossfading, its logic is based on global state
    private var weatherSound: FadingSound? = null
    private val soundsFadingOut = mutableListOf<FadingSound>()

    // Current ambience zone component
    private var currentAmbienceCmp: AmbienceSoundComponent? = null

    override fun handle(event: Event): Boolean =
        when (event) {
            is MapChangedEvent -> {
                stopAllSounds()
                currentAmbienceCmp = null
                false
            }

            is AmbienceChangeEvent -> {
                val newAmbienceCmp =
                    if (event.type == AmbienceType.NONE) {
                        null
                    } else {
                        world
                            .family { all(AmbienceSoundComponent) }
                            .firstOrNull { it[AmbienceSoundComponent].type == event.type }
                            ?.get(AmbienceSoundComponent)
                    }

                if (newAmbienceCmp != currentAmbienceCmp) {
                    logger.debug { "Ambience zone changed to ${newAmbienceCmp?.type ?: "NONE"}" }
                    currentAmbienceCmp = newAmbienceCmp
                }
                true
            }

            else -> {
                false
            }
        }

    override fun onTick() {
        // 1. Update all sounds
        activeBaseSound?.update(deltaTime, gameStateCmp.ambienceVolume)
        fadingOutBaseSound?.update(deltaTime, gameStateCmp.ambienceVolume)
        activeTimeSound?.update(deltaTime, gameStateCmp.ambienceVolume)
        fadingOutTimeSound?.update(deltaTime, gameStateCmp.ambienceVolume)
        weatherSound?.update(deltaTime, gameStateCmp.ambienceVolume)

        // Clean up sounds that have finished fading out
        if (fadingOutBaseSound?.isStopped() == true) fadingOutBaseSound = null
        if (fadingOutTimeSound?.isStopped() == true) fadingOutTimeSound = null
        soundsFadingOut.removeAll {
            it.update(deltaTime, gameStateCmp.ambienceVolume)
            it.isStopped()
        }

        // 2. Determine the desired sounds for the current state
        val variations = currentAmbienceCmp?.variations
        val volume = currentAmbienceCmp?.volume ?: 1f
        val timeOfDay = gameStateCmp.getTimeOfDay()
        val weather = gameStateCmp.weather

        val desiredBasePath = variations?.get(SoundVariation.BASE)
        val desiredTimePath = variations?.get(if (timeOfDay == TimeOfDay.DAY) SoundVariation.DAY else SoundVariation.NIGHT)
        val desiredWeatherPath = if (weather == Weather.RAIN) variations?.get(SoundVariation.RAIN) else null

        // 3. Update Base and Time layers
        val basePair = updateSoundLayer(activeBaseSound, fadingOutBaseSound, desiredBasePath, volume, FADE_IN_TIME, FADE_OUT_TIME)
        activeBaseSound = basePair.first
        fadingOutBaseSound = basePair.second

        val timePair = updateSoundLayer(activeTimeSound, fadingOutTimeSound, desiredTimePath, volume, FADE_IN_TIME, FADE_OUT_TIME)
        activeTimeSound = timePair.first
        fadingOutTimeSound = timePair.second

        // Find any weather sound, whether it's the active one or one that's currently fading out.
        // We identify it by its path, assuming it always contains "rain".
        val anyWeatherSound = weatherSound ?: soundsFadingOut.find { it.path.contains("rain") }

        if (desiredWeatherPath != null) {
            // Case A: Rain should be playing.
            if (anyWeatherSound != null) {
                // A rain sound already exists. We simply retarget it to the new zone's sound file.
                // This preserves the current volume and state (playing or fading out).
                anyWeatherSound.retargetCrossfade(desiredWeatherPath, volume, crossfadeSec = RAIN_XFADE_TIME)

                // If the sound was fading out, we need to "rescue" it from the fade-out list
                // and make it the primary active sound again, telling it to fade back in.
                if (soundsFadingOut.remove(anyWeatherSound)) {
                    weatherSound = anyWeatherSound
                    anyWeatherSound.fadeIn()
                }
            } else {
                // No rain sound exists anywhere. Create a new one.
                weatherSound =
                    FadingSound(
                        desiredWeatherPath,
                        volume,
                        weather.transitionDuration,
                        weather.transitionDuration,
                        reverb,
                        RAIN_DELAY,
                    )
            }
        } else {
            // Case B: Rain should NOT be playing.
            if (weatherSound != null) {
                // An active rain sound exists. Tell it to fade out and move it to the list.
                weatherSound!!.fadeOut()
                soundsFadingOut.add(weatherSound!!)
                weatherSound = null
            }
            // If anyWeatherSound was found in soundsFadingOut but desired path is null,
            // we do nothing. It just continues its fade-out as intended.
        }
    }

    /**
     * Manages a sound layer to enable crossfading between sounds.
     *
     * Handles transitions between active and fading-out sounds, reusing instances when possible,
     * and ensures smooth crossfades for ambience, time-of-day, and weather layers.
     *
     * @param activeSound The currently active sound instance, or null.
     * @param fadingOutSound The sound instance currently fading out, or null.
     * @param desiredPath The file path of the desired sound to play, or null to stop.
     * @param volume The target volume for the new sound.
     * @param fadeInSec Duration in seconds for fade-in.
     * @param fadeOutSec Duration in seconds for fade-out.
     * @return Pair of (new active sound, new fading-out sound).
     */
    private fun updateSoundLayer(
        activeSound: FadingSound?,
        fadingOutSound: FadingSound?,
        desiredPath: String?,
        volume: Float,
        fadeInSec: Float,
        fadeOutSec: Float,
    ): Pair<FadingSound?, FadingSound?> {
        // Case 1: desired path already active
        if (activeSound != null && activeSound.path == desiredPath) {
            activeSound.retarget(desiredPath, volume)
            return Pair(activeSound, fadingOutSound)
        }

        // Case 2: desired path is currently fading out (A -> B -> A)
        if (fadingOutSound != null && fadingOutSound.path == desiredPath) {
            // Important: current active becomes the new fading-out
            activeSound?.fadeOut()
            fadingOutSound.fadeIn() // reverses the fade-out proportionally
            return Pair(fadingOutSound, activeSound)
        }

        // --- New transition (A -> B -> C) or (A -> null) ---

        // 1) Let any previously fading sound finish naturally (NO hard stop).
        //    We only park it into the shared fading list if it exists, and we're not going to reuse it below.
        val reusable: FadingSound? = fadingOutSound

        // 2) Current active becomes the new fading-out
        val newFadingOut = activeSound
        newFadingOut?.fadeOut()

        // 3) Create or reuse the new active
        val newActive =
            if (desiredPath != null) {
                if (reusable != null) {
                    // Reuse the previous fading-out instance:
                    // remove it from the global fading list if it had been added earlier (defensive)
                    soundsFadingOut.remove(reusable)
                    reusable.retarget(desiredPath, volume)
                    reusable.fadeIn()
                    reusable
                } else {
                    FadingSound(
                        desiredPath,
                        volume,
                        fadeInSec,
                        fadeOutSec,
                        reverb,
                    )
                }
            } else {
                null
            }

        // 4) Any "older" fadingOut (the one passed in) that we did NOT reuse should keep fading in the background
        if (fadingOutSound != null && fadingOutSound !== newActive) {
            // Avoid duplicates
            if (!soundsFadingOut.contains(fadingOutSound)) {
                soundsFadingOut.add(fadingOutSound)
            }
        }

        return Pair(newActive, newFadingOut)
    }

    private fun stopAllSounds() {
        activeBaseSound?.stop()
        activeTimeSound?.stop()
        weatherSound?.stop()
        soundsFadingOut.forEach { it.stop() }
        activeBaseSound = null
        activeTimeSound = null
        weatherSound = null
        soundsFadingOut.clear()
    }

    override fun onDispose() {
        stopAllSounds()
        super.onDispose()
    }

    companion object {
        val logger = logger<AmbienceSystem>()
    }
}
