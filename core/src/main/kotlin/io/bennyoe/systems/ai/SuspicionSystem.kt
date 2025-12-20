package io.bennyoe.systems.ai

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import io.bennyoe.components.FlashlightComponent
import io.bennyoe.components.PlayerComponent
import io.bennyoe.components.PlayerStealthComponent
import io.bennyoe.components.TransformComponent
import io.bennyoe.components.ai.FieldOfViewComponent
import io.bennyoe.components.ai.FieldOfViewResultComponent
import io.bennyoe.components.ai.SuspicionComponent
import ktx.log.logger

class SuspicionSystem : IteratingSystem(family { all(SuspicionComponent, FieldOfViewResultComponent, FieldOfViewComponent) }) {
    private val playerEntity by lazy { world.family { all(PlayerComponent, PlayerStealthComponent) }.first() }
    private var detectionStrength = 0f

    override fun onTickEntity(entity: Entity) {
        val playerStealthCmp = playerEntity[PlayerStealthComponent]
        val playerTransformCmp = playerEntity[TransformComponent]
        val fieldOfViewCmp = entity[FieldOfViewComponent]
        val fieldOfViewResultCmp = entity[FieldOfViewResultComponent]
        val suspicionCmp = entity[SuspicionComponent]

        if (!fieldOfViewResultCmp.isSeeingPlayer) {
            suspicionCmp.suspiciousLevel = 0f
            detectionStrength = 0f
            logger.debug { "Player is last seen at ${suspicionCmp.lastKnownPlayerPos}" }
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

        logger.debug { "Detection Strength is: $detectionStrength" }
    }

    companion object {
        val logger = logger<SuspicionSystem>()
    }
}
