package io.bennyoe.components.ai

import com.badlogic.gdx.math.Vector2
import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

class SuspicionComponent(
    val suspicionTime: Float = 3f,
) : Component<SuspicionComponent> {
    var suspiciousLevel: Float = 0f
    var suspicionTimer: Float = suspicionTime
    var lastKnownPlayerPos: Vector2? = null

    fun resetSuspicionTimer() {
        suspicionTimer = suspicionTime
    }

    override fun type() = SuspicionComponent

    companion object : ComponentType<SuspicionComponent>()
}
