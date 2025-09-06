package io.bennyoe.systems.render

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.Vector4
import io.bennyoe.components.ShaderRenderingComponent
import io.bennyoe.lightEngine.core.Scene2dLightEngine
import kotlin.collections.iterator

/**
 * Minimal helper: centralizes shader switching + per-frame uniforms + optional noise texture.
 * No behavior changes compared to existing methods.
 */
class ShaderService {
    fun switchToDefaultIfNeeded(
        engine: Scene2dLightEngine,
        current: ShaderType,
    ): ShaderType {
        if (current != ShaderType.DEFAULT) {
            engine.batch.flush()
            engine.setShaderToDefaultShader()
        }
        return ShaderType.DEFAULT
    }

    fun switchToLightingIfNeeded(
        engine: Scene2dLightEngine,
        current: ShaderType,
    ): ShaderType {
        if (current != ShaderType.LIGHTING) {
            engine.batch.flush()
            engine.setShaderToEngineShader()
        }
        return ShaderType.LIGHTING
    }

    fun switchToCustom(
        engine: Scene2dLightEngine,
        program: ShaderProgram,
    ) {
        engine.batch.flush()
        engine.setShaderToCustomShader(program)
    }

    fun configureNoiseIfPresent(shaderCmp: ShaderRenderingComponent) {
        val noise = shaderCmp.noiseTexture ?: return
        Gdx.gl.glActiveTexture(GL20.GL_TEXTURE1)
        noise.bind()
        shaderCmp.shader?.setUniformi("u_noiseTexture", 1)
        Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0)
    }

    fun updateUniformsPerFrame(
        shaderCmp: ShaderRenderingComponent,
        timeOfDay: Float,
        continuousTime: Float,
    ) {
        val shader = shaderCmp.shader ?: return
        shader.setUniformf("u_time", timeOfDay)
        shader.setUniformf("u_continuousTime", continuousTime)
        for ((k, v) in shaderCmp.uniforms) setUniformAny(shader, k, v)
    }

    /**
     * Sets a shader uniform of any supported type.
     *
     * Supported types:
     * - `Float`, `Int`, `Boolean`
     * - `Vector2`, `Vector3`, `Vector4`
     * - `Matrix4`
     * - `Pair<Texture, Int>` for sampler2D uniforms (texture and texture unit)
     *
     * @param shader The shader program to set the uniform on.
     * @param name The name of the uniform variable.
     * @param value The value to set; must be one of the supported types.
     *
     * @throws IllegalArgumentException if the value type is not supported or if a sampler2D pair is invalid.
     */

    private fun setUniformAny(
        shader: ShaderProgram,
        name: String,
        value: Any,
    ) {
        val loc = shader.fetchUniformLocation(name, false)
        if (loc < 0) return

        when (value) {
            is Float -> shader.setUniformf(loc, value)
            is Int -> shader.setUniformi(loc, value)
            is Boolean -> shader.setUniformi(loc, if (value) 1 else 0)

            is Vector2 -> shader.setUniformf(loc, value)
            is Vector3 -> shader.setUniformf(loc, value)
            is Vector4 -> shader.setUniformf(loc, value)

            is Matrix4 -> shader.setUniformMatrix(loc, value)

            // Sampler2D: (texture, unit)
            is Pair<*, *> -> {
                val tex = value.first as? Texture
                val unit = value.second as? Int
                require(tex != null && unit != null) { "Pair<Texture, Int> expected for sampler uniform '$name'." }
                tex.bind(unit)
                shader.setUniformi(loc, unit)
            }

            else -> throw IllegalArgumentException("Unsupported uniform type for '$name': ${value::class.qualifiedName}")
        }
    }
}
