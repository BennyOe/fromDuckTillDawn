package io.bennyoe.components

import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.physics.box2d.Body
import com.badlogic.gdx.physics.box2d.Fixture
import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import io.bennyoe.systems.physic.WaterColumn

class WaterComponent(
    var tension: Float = 0.025f,
    var dampening: Float = 0.025f,
    var spread: Float = 0.25f,
    var density: Float = 1f,
    val enteredBodies: MutableSet<Body> = hashSetOf(),
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
