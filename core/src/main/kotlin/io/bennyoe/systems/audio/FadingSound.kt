package io.bennyoe.systems.audio

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.math.Interpolation
import de.pottgames.tuningfork.StreamedSoundSource
import io.bennyoe.config.GameConstants.AMBIENCE_VOLUME

const val LOOP_START = 5f
const val LOOP_END = 25f

class FadingSound(
    soundPath: String,
    private val targetVolume: Float,
    private val fadeInDuration: Float,
    private val fadeOutDuration: Float,
    private val reverbSystem: ReverbSystem,
    private val delayDuration: Float = 0f,
) {
    val source: StreamedSoundSource = StreamedSoundSource(Gdx.files.internal(soundPath))
    val path: String = soundPath
    private var delayTime = 0f
    private var fadeTime = 0f
    private var state = State.FADING_IN

    private enum class State { FADING_IN, PLAYING, FADING_OUT, STOPPED }

    init {
        source.isRelative = true
        source.setLooping(true)
        source.setLoopPoints(LOOP_START, LOOP_END)
        reverbSystem.registerSource(source)
        source.volume = 0f
        source.play()
    }

    fun update(deltaTime: Float) {
        if (state == State.STOPPED) return
        if (delayTime < delayDuration) {
            source.volume = 0f
            delayTime += deltaTime
            return
        }

        fadeTime += deltaTime
        val alphaFadeIn = (fadeTime / fadeInDuration).coerceIn(0f, 1f)
        val alphaFadeOut = (fadeTime / fadeOutDuration).coerceIn(0f, 1f)

        when (state) {
            State.FADING_IN -> {
                source.volume = Interpolation.fade.apply(0f, targetVolume * AMBIENCE_VOLUME, alphaFadeIn)
                if (alphaFadeIn >= 1f) {
                    state = State.PLAYING
                    fadeTime = 0f
                }
            }

            State.FADING_OUT -> {
                source.volume = Interpolation.fade.apply(targetVolume * AMBIENCE_VOLUME, 0f, alphaFadeOut)
                if (alphaFadeOut >= 1f) {
                    stop()
                }
            }

            State.PLAYING -> {
                // Volume is already at target, do nothing.
            }

            State.STOPPED -> {
                // Should not happen, but for safety.
            }
        }
    }

    fun isStopped(): Boolean = state == State.STOPPED

    fun fadeOut() {
        if (state == State.FADING_OUT || state == State.STOPPED) return
        state = State.FADING_OUT
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
