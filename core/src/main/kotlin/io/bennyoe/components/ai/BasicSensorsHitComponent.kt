package io.bennyoe.components.ai

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

class BasicSensorsHitComponent : Component<BasicSensorsHitComponent> {
    var canAttack = false
    var wallHit = false
    var groundHit = false
    var jumpHit = false
    var wallHeightHit = false
    var seesPlayer = false
    var playerInThrowRange = false

    override fun type() = BasicSensorsHitComponent

    companion object : ComponentType<BasicSensorsHitComponent>()
}

