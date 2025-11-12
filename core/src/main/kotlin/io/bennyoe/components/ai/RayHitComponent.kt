package io.bennyoe.components.ai

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import ktx.collections.GdxArray
import ktx.collections.gdxArrayOf

class RayHitComponent : Component<RayHitComponent> {
    var canAttack = false
    var wallHit = false
    var groundHit = false
    var jumpHit = false
    var wallHeightHit = false
    var seesPlayer = false
    var playerInThrowRange = false
    val upperLedgeHits: GdxArray<LedgeHitData> = gdxArrayOf(ordered = true)
    val lowerLedgeHits: GdxArray<LedgeHitData> = gdxArrayOf(ordered = true)

    override fun type() = RayHitComponent

    companion object : ComponentType<RayHitComponent>()
}

data class LedgeHitData(
    val hit: Boolean,
    val xCoordinate: Float,
) : Comparable<LedgeHitData> {
    override fun compareTo(other: LedgeHitData): Int = xCoordinate.compareTo(other.xCoordinate)
}
