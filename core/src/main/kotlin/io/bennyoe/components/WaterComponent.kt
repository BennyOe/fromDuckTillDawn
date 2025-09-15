package io.bennyoe.components

import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.physics.box2d.Fixture
import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import io.bennyoe.systems.physic.WaterColumn

const val WATER_DETAIL = 0.02f
const val MIN_SPLASH_AREA: Float = 0.1f
const val DRAG_MOD: Float = 0.25f
const val LIFT_MOD: Float = 0.25f
const val MAX_DRAG: Float = 2000f
const val MAX_LIFT: Float = 500f
const val TORQUE_DAMPING = 100f

class WaterComponent(
    var tension: Float = 0.025f,
    var dampening: Float = 0.025f,
    var spread: Float = 0.25f,
    var density: Float = 1f,
    val columnSeparation: Float = WATER_DETAIL,
) : Component<WaterComponent> {
    var shader: ShaderProgram? = null
    val uniforms: MutableMap<String, Any> = mutableMapOf()
    var columns: MutableList<WaterColumn> = mutableListOf() // represent the height of the waves
    var fixturePairs: MutableSet<Pair<Fixture, Fixture>> = LinkedHashSet() // contacts between this object and other dynamic bodies

    override fun type() = WaterComponent

    companion object : ComponentType<WaterComponent>()

    override fun World.onRemove(entity: Entity) {
        columns.clear()
        fixturePairs.clear()
    }
}
