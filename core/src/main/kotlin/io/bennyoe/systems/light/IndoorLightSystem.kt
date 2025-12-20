package io.bennyoe.systems.light

import com.badlogic.gdx.math.Interpolation
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import io.bennyoe.components.GameStateComponent
import io.bennyoe.components.LightComponent
import io.bennyoe.lightEngine.core.GameLight

const val LIGHT_INDOOR_TRANSITION_DURATION = .5f
const val LIGHT_INDOOR_FADE_IN_DELAY = .0f

class IndoorLightSystem : IteratingSystem(family { all(LightComponent) }) {
    private val gameStateCmp by lazy { world.family { all(GameStateComponent) }.first()[GameStateComponent] }

    private enum class GlobalState { ON, FADING_IN, DELAY_BEFORE_FADE_IN, FADING_OUT, OFF }

    private var state = GlobalState.OFF
    private var fadeTimer = 0f
    private var fadeInDelayTimer = 0f

    override fun onUpdate() {
        val playerIsIndoor = gameStateCmp.playerIsIndoor

        // --- State Transition Logic ---
        if (playerIsIndoor && (state == GlobalState.OFF || state == GlobalState.FADING_OUT)) {
            // Player entered or re-entered while fading out. Start the delay.
            state = GlobalState.DELAY_BEFORE_FADE_IN
            fadeInDelayTimer = LIGHT_INDOOR_FADE_IN_DELAY
        } else if (!playerIsIndoor &&
            (state == GlobalState.ON || state == GlobalState.FADING_IN || state == GlobalState.DELAY_BEFORE_FADE_IN)
        ) {
            // Player left. Start fading out immediately from the current brightness.
            val progress = fadeTimer / LIGHT_INDOOR_TRANSITION_DURATION
            state = GlobalState.FADING_OUT
            fadeTimer = progress * LIGHT_INDOOR_TRANSITION_DURATION
        }

        // --- Timer Update Logic ---
        when (state) {
            GlobalState.DELAY_BEFORE_FADE_IN -> {
                fadeInDelayTimer -= deltaTime
                if (fadeInDelayTimer <= 0f) {
                    // Delay is over, start the actual fade-in from zero.
                    state = GlobalState.FADING_IN
                    fadeTimer = 0f
                }
            }

            GlobalState.FADING_IN -> {
                fadeTimer = (fadeTimer + deltaTime).coerceAtMost(LIGHT_INDOOR_TRANSITION_DURATION)
                if (fadeTimer >= LIGHT_INDOOR_TRANSITION_DURATION) {
                    state = GlobalState.ON
                }
            }

            GlobalState.FADING_OUT -> {
                fadeTimer = (fadeTimer - deltaTime).coerceAtLeast(0f)
                if (fadeTimer <= 0f) {
                    state = GlobalState.OFF
                }
            }

            else -> {} // ON and OFF states don't need timer updates
        }

        super.onUpdate()
    }

    override fun onTickEntity(entity: Entity) {
        val gameLight = entity[LightComponent].gameLight
        if (!gameLight.isIndoor) return

        val fadeAlpha = (fadeTimer / LIGHT_INDOOR_TRANSITION_DURATION).coerceIn(0f, 1f)
        gameLight.distanceScale =
            when (state) {
                GlobalState.ON -> 1f
                GlobalState.FADING_IN -> Interpolation.slowFast.apply(0f, 1f, fadeAlpha)
                GlobalState.FADING_OUT -> Interpolation.fade.apply(0f, 1f, fadeAlpha)
                else -> 0f
            }

        val alpha = (fadeTimer / LIGHT_INDOOR_TRANSITION_DURATION).coerceIn(0f, 1f)
        when (state) {
            GlobalState.ON -> {
                gameLight.setOn(true)
                when (gameLight) {
                    is GameLight.Point -> {
                        gameLight.distance = gameLight.baseDistance
                    }

                    is GameLight.Spot -> {
                        gameLight.distance = gameLight.baseDistance
                    }

                    else -> {}
                }
            }

            GlobalState.FADING_IN -> {
                gameLight.setOn(true)
                val fadedDistance = Interpolation.slowFast.apply(0f, gameLight.baseDistance, alpha)
                when (gameLight) {
                    is GameLight.Point -> {
                        gameLight.distance = fadedDistance
                    }

                    is GameLight.Spot -> {
                        gameLight.distance = fadedDistance
                    }

                    else -> {}
                }
            }

            GlobalState.FADING_OUT -> {
                gameLight.setOn(true)
                val fadedDistance = Interpolation.fade.apply(0f, gameLight.baseDistance, alpha)
                when (gameLight) {
                    is GameLight.Point -> {
                        gameLight.distance = fadedDistance
                    }

                    is GameLight.Spot -> {
                        gameLight.distance = fadedDistance
                    }

                    else -> {}
                }
            }

            GlobalState.DELAY_BEFORE_FADE_IN, GlobalState.OFF -> {
                gameLight.setOn(false)
            }
        }
    }
}
