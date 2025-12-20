package io.bennyoe.components.ai

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import ktx.collections.GdxArray
import ktx.collections.gdxArrayOf

class LedgeSensorsHitComponent : Component<LedgeSensorsHitComponent> {
    val upperLedgeHits: GdxArray<LedgeHitData> = gdxArrayOf(ordered = true)
    val lowerLedgeHits: GdxArray<LedgeHitData> = gdxArrayOf(ordered = true)

    override fun type() = LedgeSensorsHitComponent

    companion object : ComponentType<LedgeSensorsHitComponent>()
}

data class LedgeHitData(
    val hit: Boolean,
    val xCoordinate: Float,
) : Comparable<LedgeHitData> {
    override fun compareTo(other: LedgeHitData): Int = xCoordinate.compareTo(other.xCoordinate)
}
