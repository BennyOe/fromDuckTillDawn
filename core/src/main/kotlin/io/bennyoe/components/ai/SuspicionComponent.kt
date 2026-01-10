package io.bennyoe.components.ai

import com.badlogic.gdx.math.Vector2
import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

class SuspicionComponent(
    val suspicionRememberTime: Float = 3f,
    val noiseRememberTime: Float = 2f,
    val heardWeightFactor: Float = 0.6f,
    val visionWeightFactor: Float = 0.8f,
) : Component<SuspicionComponent> {
    var noiseEventStrength: Float = 0f
    var visionSuspicionStrength: Float = 0f
    var hearingSuspicionStrength: Float = 0f
    var combinedSuspicionStrength: Float = 0f
    var suspicionHoldElapsedTime: Float = 0f
    var noiseElapsedTime: Float = 0f
    var lastKnownPlayerPos: Vector2? = null

    fun resetSuspicionHoldElapsedTime() {
        suspicionHoldElapsedTime = 0f
    }

    fun resetNoiseElapsedTime() {
        noiseElapsedTime = 0f
    }

    override fun type() = SuspicionComponent

    companion object : ComponentType<SuspicionComponent>()
}
