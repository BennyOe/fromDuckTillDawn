package io.bennyoe.systems

import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.math.MathUtils
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import io.bennyoe.components.GameStateComponent
import io.bennyoe.components.ParticleComponent
import io.bennyoe.components.RainComponent
import io.bennyoe.components.Weather

class RainSystem : IteratingSystem(family { all(RainComponent, ParticleComponent) }) {
    private val gameStateCmp by lazy { world.family { all(GameStateComponent) }.first()[GameStateComponent] }
    private var transitionTime = 0f
    private var waitTime = 0f
    private var effectIsSetup = false

    companion object {
        const val WAIT_DURATION = 22f
        const val FADE_DURATION = 22f
        const val MAX_EMISSION = 1200f
    }

    override fun onTickEntity(entity: Entity) {
        val particleCmp = entity[ParticleComponent]
        val isRaining = gameStateCmp.weather == Weather.RAIN

        if (isRaining) {
            if (!effectIsSetup) {
                setupRainEffect(particleCmp)
            }

            if (waitTime < WAIT_DURATION) {
                waitTime += deltaTime
                return
            }

            if (transitionTime < FADE_DURATION) {
                transitionTime += deltaTime
                val t = (transitionTime / FADE_DURATION).coerceAtMost(1f)
                val rate = MathUtils.lerp(0f, MAX_EMISSION, Interpolation.ElasticIn.pow2In.apply(t))

                particleCmp.actor.effect.emitters.forEach { e ->
                    e.emission.highMin = rate
                    e.emission.highMax = rate
                }
                particleCmp.enabled = true
                particleCmp.actor.effect.start()
            }
        } else {
            if (transitionTime > 0f) {
                transitionTime -= deltaTime
                val t = (transitionTime / FADE_DURATION).coerceAtMost(1f)
                val rate = MathUtils.lerp(0f, MAX_EMISSION, Interpolation.ElasticIn.pow2In.apply(t))

                particleCmp.actor.effect.emitters.forEach { e ->
                    e.emission.highMin = rate
                    e.emission.highMax = rate
                }
                particleCmp.actor.effect.start()
            } else {
                waitTime = 0f
                particleCmp.enabled = false
                effectIsSetup = false
            }
        }
    }

    private fun setupRainEffect(particleCmp: ParticleComponent) {
        particleCmp.actor.effect.reset(false)
        transitionTime = 0f
        particleCmp.actor.effect.emitters.forEach { e ->
            e.minParticleCount = 0
            e.emission.highMin = 0f
            e.emission.highMax = 0f
        }
        effectIsSetup = true
    }
}
