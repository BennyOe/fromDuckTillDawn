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

/**
 * System responsible for managing AI suspicion logic.
 *
 * Combines vision and hearing inputs to calculate detection strength for entities
 * with `SuspicionComponent`. Handles noise events, updates last known player position,
 * and manages detection decay and UI label updates.
 *
 */
class SuspicionSystem :
    IteratingSystem(
        family {
            all(SuspicionComponent, FieldOfViewResultComponent, FieldOfViewComponent, HearingComponent)
        },
    ),
    EventListener {
    private val playerEntity by lazy { world.family { all(PlayerComponent, PlayerStealthComponent) }.first() }
    private val hearingFamily = world.family { all(SuspicionComponent, HearingComponent, TransformComponent) }

    override fun onTickEntity(entity: Entity) {
        val playerStealthCmp = playerEntity[PlayerStealthComponent]
        val playerTransformCmp = playerEntity[TransformComponent]
        val fieldOfViewCmp = entity[FieldOfViewComponent]
        val fieldOfViewResultCmp = entity[FieldOfViewResultComponent]
        val suspicionCmp = entity[SuspicionComponent]
        val stealthLabelCmp = entity[StealthLabelComponent]

        if (suspicionCmp.noiseEventStrength > 0f) {
            suspicionCmp.noiseElapsedTime += deltaTime

            // If time is over -> forget noise completely
            if (suspicionCmp.noiseElapsedTime >= suspicionCmp.noiseRememberTime) {
                suspicionCmp.noiseEventStrength = 0f
                suspicionCmp.noiseElapsedTime = 0f
            }
        } else {
            // no noise currently remembered
            suspicionCmp.noiseElapsedTime = 0f
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
            suspicionCmp.visionSuspicionStrength = (baseSeen * illuminationOfPlayerNorm).coerceIn(0f, 1f)
        } else {
            suspicionCmp.visionSuspicionStrength = 0f
        }

        // --- Combine hearing + vision into final detectionStrength ---
        val hearRememberFactor = (1f - (suspicionCmp.noiseElapsedTime / suspicionCmp.noiseRememberTime)).coerceIn(0f, 1f)
        suspicionCmp.hearingSuspicionStrength = (suspicionCmp.noiseEventStrength * hearRememberFactor).coerceIn(0f, 1f)

        val newDetectionStrength =
            (
                suspicionCmp.visionSuspicionStrength * suspicionCmp.visionWeightFactor +
                    suspicionCmp.hearingSuspicionStrength * suspicionCmp.heardWeightFactor
            ).coerceIn(
                0f,
                1f,
            )

        val decaySpeedPerSecond = 0.35f

        if (newDetectionStrength > suspicionCmp.combinedSuspicionStrength) {
            // rising -> follow immediately and reset hold timer
            suspicionCmp.combinedSuspicionStrength = newDetectionStrength
            suspicionCmp.resetSuspicionHoldElapsedTime()
        } else if (newDetectionStrength < suspicionCmp.combinedSuspicionStrength) {
            // falling -> hold first, then decay down
            if (suspicionCmp.suspicionHoldElapsedTime < suspicionCmp.suspicionRememberTime) {
                suspicionCmp.suspicionHoldElapsedTime += deltaTime
                // hold: keep detectionStrength as-is
            } else {
                // decay towards newDetectionStrength, but never go below it
                val decayed = suspicionCmp.combinedSuspicionStrength - decaySpeedPerSecond * deltaTime
                suspicionCmp.combinedSuspicionStrength = maxOf(newDetectionStrength, decayed)
            }
        } else {
            suspicionCmp.resetSuspicionHoldElapsedTime()
        }

        if (suspicionCmp.noiseEventStrength == 0f && suspicionCmp.visionSuspicionStrength == 0f &&
            suspicionCmp.combinedSuspicionStrength == 0f
        ) {
            stealthLabelCmp.label.setText("")
        } else {
            stealthLabelCmp.label.setText(
                "Detection Strength is ${"%.2f".format(suspicionCmp.combinedSuspicionStrength)} " +
                    "| vision=${"%.2f".format(suspicionCmp.visionSuspicionStrength)} " +
                    "| heard=${"%.2f".format(suspicionCmp.hearingSuspicionStrength)}",
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
            val suspicionCmp = entity[SuspicionComponent]

            val distanceSq = transformCmp.position.dst2(event.pos)
            val hearingRadiusSq = hearingCmp.hearingRadius * hearingCmp.hearingRadius
            val eventRangeSq = event.range * event.range

            if (distanceSq > hearingRadiusSq || distanceSq > eventRangeSq) {
                return@forEach
            }

            val distanceNorm = (1f - (distanceSq / eventRangeSq)).coerceIn(0f, 1f)
            val noiseStrength = distanceNorm.coerceIn(0f, 1f)

            // strongest noise wins policy. If it should be the latest noise wins -> remove the if-condition
            if (noiseStrength > suspicionCmp.noiseEventStrength) {
                suspicionCmp.noiseEventStrength = noiseStrength
                suspicionCmp.resetNoiseElapsedTime()
                // keep last known player pos even if only heard
                suspicionCmp.lastKnownPlayerPos = event.pos.cpy()
            }

            logger.debug { "Noise heard: strength=$noiseStrength | dist=${kotlin.math.sqrt(distanceSq)}" }
        }
    }

    companion object {
        val logger = logger<SuspicionSystem>()
    }
}
