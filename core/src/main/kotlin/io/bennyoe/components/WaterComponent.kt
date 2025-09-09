package io.bennyoe.components

import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

class WaterComponent : Component<WaterComponent> {
    var shader: ShaderProgram? = null
    val uniforms: MutableMap<String, Any> = mutableMapOf()

    override fun type() = WaterComponent

    companion object : ComponentType<WaterComponent>()
}
