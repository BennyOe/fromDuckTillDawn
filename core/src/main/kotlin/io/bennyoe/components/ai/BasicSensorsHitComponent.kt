package io.bennyoe.components.ai

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import io.bennyoe.utility.SensorType

class BasicSensorsHitComponent : Component<BasicSensorsHitComponent> {
    val sensorHitMap: MutableMap<SensorType, SensorHit> = mutableMapOf()

    fun getSensorHit(sensorType: SensorType) = sensorHitMap[sensorType]?.isHit ?: false

    fun setSensorHit(
        sensorType: SensorType,
        isHit: Boolean,
    ) {
        sensorHitMap[sensorType] = SensorHit(isHit)
    }

    override fun type() = BasicSensorsHitComponent

    companion object : ComponentType<BasicSensorsHitComponent>()
}

data class SensorHit(
    var isHit: Boolean,
)
