package io.bennyoe.systems.audio

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.math.Interpolation
import de.pottgames.tuningfork.StreamedSoundSource

const val LOOP_START = 5f
const val LOOP_END = 25f

class FadingSound(
    soundPath: String,
    private var targetVolume: Float,
    private val fadeInDuration: Float,
    private val fadeOutDuration: Float,
    private val reverbSystem: ReverbSystem,
    private val delayDuration: Float = 0f,
) {
    var source: StreamedSoundSource = StreamedSoundSource(Gdx.files.internal(soundPath))
        private set
    var path: String = soundPath
        private set

    private var delayTime = 0f
    private var fadeTime = 0f
    private var state = State.FADING_IN
    private var fadeStartVolume = 0f

    private var ghostSource: StreamedSoundSource? = null
    private var xfadeTime = 0f
    private var xfadeDuration = 0f

    // start volume of the old (ghost)
    private var xfadeOldStartVol = 0f

    // start volume of the new (usually 0)
    private var xfadeNewStartVol = 0f

    private enum class State { FADING_IN, PLAYING, FADING_OUT, STOPPED }

    init {
        setupSource()
        source.play()
    }

    private fun setupSource() {
        source.isRelative = true
        source.setLooping(true)
        source.setLoopPoints(LOOP_START, LOOP_END)
        reverbSystem.registerSource(source)
        source.volume = 0f
    }

    /**
     * Logic to retarget the sound source while preserving the fade state.
     *
     * If the new sound path is the same as the current, only the target volume is updated.
     * Otherwise, replaces the current sound source with a new one, transferring playback state and volume,
     * and cleans up the old source. Ensures seamless transition without abrupt volume changes.
     *
     * @param newSoundPath Path to the new sound file.
     * @param newTargetVolume Desired target volume for the new sound.
     */
    fun retarget(
        newSoundPath: String,
        newTargetVolume: Float,
    ) {
        if (path == newSoundPath) {
            // If path is the same, just update the target volume
            targetVolume = newTargetVolume
            return
        }

        // Create new source
        val newSource = StreamedSoundSource(Gdx.files.internal(newSoundPath))
        path = newSoundPath
        targetVolume = newTargetVolume

        // Transfer state from the old source to the new one
        newSource.isRelative = true
        newSource.setLooping(true)
        newSource.setLoopPoints(LOOP_START, LOOP_END)
        newSource.volume = source.volume
        reverbSystem.registerSource(newSource)
        newSource.play()

        // Clean up the old source
        reverbSystem.unregisterSource(source)
        source.dispose()

        // Replace the internal source
        source = newSource
    }

    /**
     * Retarget with a short internal crossfade to avoid volume jumps.
     */
    fun retargetCrossfade(
        newSoundPath: String,
        newTargetVolume: Float,
        crossfadeSec: Float = 0.25f,
    ) {
        if (path == newSoundPath) { // same file: just adjust target
            targetVolume = newTargetVolume
            return
        }

        // 1) Prepare new live source
        val newSrc =
            StreamedSoundSource(Gdx.files.internal(newSoundPath)).apply {
                isRelative = true
                setLooping(true)
                setLoopPoints(LOOP_START, LOOP_END)
                volume = 0f // start from silence, we fade it in
                reverbSystem.registerSource(this)
                play()
            }

        // 2) If we were already crossfading, clean up the previous ghost immediately
        ghostSource?.let {
            reverbSystem.unregisterSource(it)
            it.dispose()
        }

        // 3) Current source becomes the ghost (if it exists and is not the same)
        val old = source
        ghostSource = old
        xfadeOldStartVol = old.volume

        // 4) Swap to the new main source
        source = newSrc
        path = newSoundPath
        targetVolume = newTargetVolume
        xfadeNewStartVol = 0f
        xfadeTime = 0f
        xfadeDuration = crossfadeSec
    }

    /**
     * Updates the fading sound state, handling fade-in, fade-out, and crossfade logic.
     *
     * @param deltaTime Time elapsed since the last update, in seconds.
     * @param ambienceVolume Multiplier for the target volume, typically representing global ambience.
     *
     * - If the sound is stopped, does nothing.
     * - Handles initial delay before starting fade-in.
     * - Manages fade-in and fade-out transitions using interpolation.
     * - Maintains steady volume during playing state.
     * - If a crossfade is active, interpolates both the old (ghost) and new sources,
     *   ensuring smooth volume transitions and cleaning up the ghost source when done.
     */
    fun update(
        deltaTime: Float,
        ambienceVolume: Float,
    ) {
        if (state == State.STOPPED) return
        if (delayTime < delayDuration) {
            source.volume = 0f
            delayTime += deltaTime
            return
        }

        fadeTime += deltaTime
        val alphaIn = (fadeTime / fadeInDuration).coerceIn(0f, 1f)
        val alphaOut = (fadeTime / fadeOutDuration).coerceIn(0f, 1f)
        val target = targetVolume * ambienceVolume

        when (state) {
            State.FADING_IN -> {
                // interpolate from fadeStartVolume -> target
                source.volume = Interpolation.fade.apply(fadeStartVolume, target, alphaIn)
                if (alphaIn >= 1f) {
                    state = State.PLAYING
                    fadeTime = 0f
                }
            }

            State.FADING_OUT -> {
                // interpolate from fadeStartVolume -> 0
                source.volume = Interpolation.fade.apply(fadeStartVolume, 0f, alphaOut)
                if (alphaOut >= 1f) stop()
            }

            State.PLAYING -> {
                // steady follow target
                source.volume = target
            }

            State.STOPPED -> { // no-op
            }
        }

        ghostSource?.let { ghost ->
            xfadeTime += deltaTime
            val a = (xfadeTime / xfadeDuration).coerceIn(0f, 1f)

            // old → 0
            ghost.volume = Interpolation.fade.apply(xfadeOldStartVol, 0f, a)

            // new 0 → target (respect current state/ambience)
            val target = targetVolume * ambienceVolume
            val newVol = Interpolation.fade.apply(xfadeNewStartVol, target, a)

            // If we are also in FADING_IN/FADING_OUT, pick the LOWER of both envelopes to avoid overshoot
            source.volume =
                if (state == State.FADING_IN || state == State.FADING_OUT) {
                    minOf(source.volume, newVol)
                } else {
                    newVol
                }

            if (a >= 1f) {
                reverbSystem.unregisterSource(ghost)
                ghost.dispose()
                ghostSource = null
            }
        }
    }

    fun isStopped(): Boolean = state == State.STOPPED

    fun fadeOut() {
        if (state == State.FADING_OUT || state == State.STOPPED) return
        fadeStartVolume = source.volume
        state = State.FADING_OUT
        fadeTime = 0f
    }

    fun fadeIn() {
        fadeStartVolume = source.volume
        state = State.FADING_IN
        fadeTime = 0f
    }

    fun stop() {
        if (state != State.STOPPED) {
            reverbSystem.unregisterSource(source)
            source.dispose()
            state = State.STOPPED
        }
    }
}
