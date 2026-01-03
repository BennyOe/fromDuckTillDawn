package io.bennyoe.systems

import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.quillraven.fleks.IntervalSystem
import com.github.quillraven.fleks.World.Companion.inject
import io.bennyoe.components.GameMood
import io.bennyoe.components.GameStateComponent
import io.bennyoe.components.NoiseProfileComponent
import io.bennyoe.components.TransformComponent
import io.bennyoe.event.NoiseEvent
import io.bennyoe.event.PlaySoundEvent
import io.bennyoe.event.fire

/**
 * Translates `PlaySoundEvent` into `NoiseEvent` for the gameplay world.
 *
 * Listens for `PlaySoundEvent`, calculates noise properties using `NoiseProfileComponent`,
 * and emits `NoiseEvent` if the range is valid. Enables AI or other systems to react to noise.
 */
class NoiseEmitterSystem(
    val stage: Stage = inject("stage"),
) : IntervalSystem(),
    EventListener {
    private val gameStateCmp by lazy { world.family { all(GameStateComponent) }.first()[GameStateComponent] }

    override fun handle(event: Event): Boolean {
        if (gameStateCmp.gameMood != GameMood.STEALTH) return false
        return when (event) {
            is PlaySoundEvent -> {
                val noiseProfileCmp = event.entity.getOrNull(NoiseProfileComponent)
                val transformCmp = event.entity.getOrNull(TransformComponent)

                if (noiseProfileCmp != null && transformCmp != null) {
                    val pos = event.position ?: transformCmp.position
                    noiseProfileCmp.noises[event.soundType]?.let { settings ->

                        val finalRange = settings.range * noiseProfileCmp.noiseMultiplier

                        if (finalRange > 0) {
                            stage.fire(
                                NoiseEvent(
                                    entity = event.entity,
                                    pos = pos,
                                    range = finalRange,
                                    type = settings.type,
                                    continuous = settings.continuous,
                                ),
                            )
                        }
                    }
                }
                true
            }

            else -> false
        }
    }

    override fun onTick() {
    }
}
