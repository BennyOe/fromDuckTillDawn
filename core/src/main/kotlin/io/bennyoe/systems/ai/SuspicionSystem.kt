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
import io.bennyoe.components.ai.SuspicionComponent
import io.bennyoe.event.NoiseEvent
import ktx.log.logger
const val NOISE_REMEMBER_TIME = 2f

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
    private val hearingFamily = world.family { all(SuspicionComponent, HearingComponent, TransformComponent, StealthLabelComponent) }

    override fun onTickEntity(entity: Entity) {
        val playerStealthCmp = playerEntity[PlayerStealthComponent]
        val playerTransformCmp = playerEntity[TransformComponent]
        val fieldOfViewCmp = entity[FieldOfViewComponent]
        val fieldOfViewResultCmp = entity[FieldOfViewResultComponent]
        val suspicionCmp = entity[SuspicionComponent]
        val stealthLabelCmp = entity[StealthLabelComponent]

        if (heardNoise == 0f && visionStrength == 0f && detectionStrength == 0f) stealthLabelCmp.label.setText("")

        if (heardNoise > 0f) {
            noiseRememberCounter += deltaTime

            // linear decay from 1..0 over NOISE_REMEMBER_TIME
            val rememberFactor = (1f - (noiseRememberCounter / NOISE_REMEMBER_TIME)).coerceIn(0f, 1f)
            val heardStrength = (heardNoise * rememberFactor).coerceIn(0f, 1f)

            // If time is over -> forget noise completely
            if (noiseRememberCounter >= NOISE_REMEMBER_TIME) {
                heardNoise = 0f
                noiseRememberCounter = 0f
            }

            // keep last known player pos even if only heard
            suspicionCmp.lastKnownPlayerPos = playerTransformCmp.position.cpy()

            // If we don't see the player, we still want hearing to contribute
            if (!fieldOfViewResultCmp.isSeeingPlayer) {
                val detectionFromHearing = (heardStrength * HEARD_WEIGHT_FACTOR).coerceIn(0f, 1f)
                detectionStrength = detectionFromHearing

                stealthLabelCmp.label.setText(
                    "Detection Strength is ${"%.2f".format(detectionStrength)} " +
                        "| vision=${"%.2f".format(visionStrength)} " +
                        "| heard=${"%.2f".format(heardStrength)}",
                )
                return
            }
        } else {
            // no noise currently remembered
            noiseRememberCounter = 0f

        if (!fieldOfViewResultCmp.isSeeingPlayer) {
            suspicionCmp.suspiciousLevel = 0f
            detectionStrength = 0f
//            logger.debug { "Player is last seen at ${suspicionCmp.lastKnownPlayerPos}" }
            return
        }
        // normalized values
        val distanceToPlayerNorm = (1f - (fieldOfViewResultCmp.distanceToPlayer / fieldOfViewCmp.maxDistance)).coerceIn(0f, 1f)
        val raysHittingPlayerNorm = (fieldOfViewResultCmp.raysHitting / fieldOfViewCmp.numberOfRays.toFloat()).coerceIn(0f, 1f)

        // if flashlight is on -> player is fully illuminated
        val illuminationOfPlayerNorm = playerStealthCmp.illumination
        // TODO: here is the noise level of the player

        suspicionCmp.lastKnownPlayerPos = playerTransformCmp.position.cpy()

        val baseSeen = raysHittingPlayerNorm * distanceToPlayerNorm
        val lightingBoost = illuminationOfPlayerNorm // min 0.25, max 1.0
        detectionStrength = (baseSeen * lightingBoost).coerceIn(0f, 1f)
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
