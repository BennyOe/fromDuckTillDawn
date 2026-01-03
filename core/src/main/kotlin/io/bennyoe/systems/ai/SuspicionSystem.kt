package io.bennyoe.systems.ai

import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import io.bennyoe.components.PlayerComponent
import io.bennyoe.components.PlayerStealthComponent
import io.bennyoe.components.TransformComponent
import io.bennyoe.components.ai.FieldOfViewComponent
import io.bennyoe.components.ai.FieldOfViewResultComponent
import io.bennyoe.components.ai.HearingComponent
import io.bennyoe.components.ai.StealthLabelComponent
import io.bennyoe.components.ai.SuspicionComponent
import io.bennyoe.event.NoiseEvent
import ktx.log.logger

private const val NOISE_REMEMBER_TIME = 2f
private const val SUSPICION_REMEMBER_TIME = 3f
private const val HEARD_WEIGHT_FACTOR = 0.6f
private const val SEE_WEIGHT_FACTOR = 0.8f

class SuspicionSystem :
    IteratingSystem(
        family {
            all(SuspicionComponent, FieldOfViewResultComponent, FieldOfViewComponent, HearingComponent)
        },
    ),
    EventListener {
    private val playerEntity by lazy { world.family { all(PlayerComponent, PlayerStealthComponent) }.first() }
    private var detectionStrength = 0f
    private var heardNoise = 0f
    private var noiseRememberCounter = 0f
    private var suspicionRememberCounter = 0f
    private var heardStrength = 0f
    private var visionStrength = 0f

    private val hearingFamily = world.family { all(SuspicionComponent, HearingComponent, TransformComponent, StealthLabelComponent) }

    override fun onTickEntity(entity: Entity) {
        val playerStealthCmp = playerEntity[PlayerStealthComponent]
        val playerTransformCmp = playerEntity[TransformComponent]
        val fieldOfViewCmp = entity[FieldOfViewComponent]
        val fieldOfViewResultCmp = entity[FieldOfViewResultComponent]
        val suspicionCmp = entity[SuspicionComponent]
        val stealthLabelCmp = entity[StealthLabelComponent]

        if (heardNoise > 0f) {
            noiseRememberCounter += deltaTime

            // If time is over -> forget noise completely
            if (noiseRememberCounter >= NOISE_REMEMBER_TIME) {
                heardNoise = 0f
                noiseRememberCounter = 0f
            }

            // keep last known player pos even if only heard
            suspicionCmp.lastKnownPlayerPos = playerTransformCmp.position.cpy()
        } else {
            // no noise currently remembered
            noiseRememberCounter = 0f
        }

        // --- vision calculation ---
        if (fieldOfViewResultCmp.isSeeingPlayer) {
            val distanceToPlayerNorm =
                (1f - (fieldOfViewResultCmp.distanceToPlayer / fieldOfViewCmp.maxDistance)).coerceIn(0f, 1f)
            val raysHittingPlayerNorm =
                (fieldOfViewResultCmp.raysHitting / fieldOfViewCmp.numberOfRays.toFloat()).coerceIn(0f, 1f)

            val illuminationOfPlayerNorm = playerStealthCmp.illumination

            suspicionCmp.lastKnownPlayerPos = playerTransformCmp.position.cpy()

            val baseSeen = raysHittingPlayerNorm * distanceToPlayerNorm
            visionStrength = (baseSeen * illuminationOfPlayerNorm).coerceIn(0f, 1f)
        } else {
            visionStrength = 0f
        }

        // --- Combine hearing + vision into final detectionStrength ---
        val hearRememberFactor = (1f - (noiseRememberCounter / NOISE_REMEMBER_TIME)).coerceIn(0f, 1f)
        heardStrength = (heardNoise * hearRememberFactor).coerceIn(0f, 1f)

        val newDetectionStrength = (visionStrength * SEE_WEIGHT_FACTOR + heardStrength * HEARD_WEIGHT_FACTOR).coerceIn(0f, 1f)

        val decaySpeedPerSecond = 0.35f

        if (newDetectionStrength > detectionStrength) {
            // rising -> follow immediately and reset hold timer
            detectionStrength = newDetectionStrength
            suspicionRememberCounter = 0f
        } else if (newDetectionStrength < detectionStrength) {
            // falling -> hold first, then decay down
            if (suspicionRememberCounter < SUSPICION_REMEMBER_TIME) {
                suspicionRememberCounter += deltaTime
                // hold: keep detectionStrength as-is
            } else {
                // decay towards newDetectionStrength, but never go below it
                val decayed = detectionStrength - decaySpeedPerSecond * deltaTime
                detectionStrength = maxOf(newDetectionStrength, decayed)
            }
        } else {
            suspicionRememberCounter = 0f
        }

        if (heardNoise == 0f && visionStrength == 0f && detectionStrength == 0f) {
            stealthLabelCmp.label.setText("")
        } else {
            stealthLabelCmp.label.setText(
                "Detection Strength is ${"%.2f".format(detectionStrength)} " +
                    "| vision=${"%.2f".format(visionStrength)} " +
                    "| heard=${"%.2f".format(heardStrength)}",
            )
        }
    }

    override fun handle(event: Event): Boolean =
        when (event) {
            is NoiseEvent -> {
                handleNoiseEvent(event)
                true
            }

            else -> false
        }

    private fun handleNoiseEvent(event: NoiseEvent) {
        hearingFamily.forEach { entity ->
            val transformCmp = entity[TransformComponent]
            val hearingCmp = entity[HearingComponent]

            val distanceSq = transformCmp.position.dst2(event.pos)
            val hearingRadiusSq = hearingCmp.hearingRadius * hearingCmp.hearingRadius
            val eventRangeSq = event.range * event.range

            if (distanceSq > hearingRadiusSq || distanceSq > eventRangeSq) {
                return@forEach
            }

            val distanceNorm = (1f - (distanceSq / eventRangeSq)).coerceIn(0f, 1f)
            val noiseStrength = distanceNorm.coerceIn(0f, 1f)

            if (noiseStrength > heardNoise) {
                heardNoise = noiseStrength
            }
            noiseRememberCounter = 0f

            logger.debug { "Noise heard: strength=$noiseStrength | dist=${kotlin.math.sqrt(distanceSq)}" }
        }
    }

    companion object {
        val logger = logger<SuspicionSystem>()
    }
}
