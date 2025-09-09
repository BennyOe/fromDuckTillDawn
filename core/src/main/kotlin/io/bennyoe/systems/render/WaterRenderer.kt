package io.bennyoe.systems.render

import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.quillraven.fleks.Family
import io.bennyoe.components.TransformComponent
import io.bennyoe.components.WaterComponent
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.component3
import kotlin.collections.component4

class WaterRenderer(
    val stage: Stage,
) {
    private val shaderService = ShaderService()

    fun render(
        waterFamily: Family,
        continuousTime: Float,
        tex: Texture,
        orthoCam: OrthographicCamera,
    ) {
        stage.batch.begin()
        stage.batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        waterFamily.forEach { e ->
            val water = e[WaterComponent]
            val tr = e[TransformComponent]

            // Activate and configure the water shader
            stage.batch.shader = water.shader
            water.shader?.bind()

            shaderService.updateUniformsPerFrame(
                shader = water.shader!!,
                uniforms = water.uniforms,
                continuousTime = continuousTime,
            )

            val (sx, sy, sw, sh) =
                worldRectToTexRegion(
                    tr.position.x,
                    tr.position.y,
                    tr.width,
                    tr.height,
                    orthoCam,
                    tex.width,
                    tex.height,
                )

            // Draw the specific region of the FBO texture with the water shader
            stage.batch.draw(
                tex,
                tr.position.x,
                tr.position.y,
                tr.width,
                tr.height,
                sx,
                sy,
                sw,
                sh,
                false,
                true,
            )
        }
        stage.batch.shader = null
        stage.batch.end()
    }

    private fun worldRectToTexRegion(
        rectX: Float,
        rectY: Float,
        rectW: Float,
        rectH: Float,
        cam: OrthographicCamera,
        texW: Int,
        texH: Int,
    ): IntArray {
        // visible world-area of FBO to world coordinates
        val viewW = cam.viewportWidth * cam.zoom
        val viewH = cam.viewportHeight * cam.zoom
        val viewX = cam.position.x - viewW * 0.5f
        val viewY = cam.position.y - viewH * 0.5f

        // normalized u,v in [0..1] relative to FBO
        val u0 = ((rectX - viewX) / viewW).coerceIn(0f, 1f)
        val v0 = ((rectY - viewY) / viewH).coerceIn(0f, 1f)
        val u1 = (((rectX + rectW) - viewX) / viewW).coerceIn(0f, 1f)
        val v1 = (((rectY + rectH) - viewY) / viewH).coerceIn(0f, 1f)

        // convert to texel
        val sx = (u0 * texW).toInt()
        val sy = (v0 * texH).toInt()
        val sw = ((u1 - u0) * texW).toInt().coerceAtLeast(1)
        val sh = ((v1 - v0) * texH).toInt().coerceAtLeast(1)

        return intArrayOf(sx, sy, sw, sh)
    }
}
