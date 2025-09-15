package io.bennyoe.systems.render

import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.quillraven.fleks.Family
import io.bennyoe.components.TransformComponent
import io.bennyoe.components.WaterComponent

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
        stage.viewport.apply()

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

            // Use additive blending for the water effect (GL\_ONE, GL\_ZERO) to overwrite destination with source color
            stage.batch.setBlendFunction(GL20.GL_ONE, GL20.GL_ZERO)

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

            // The blend function is reset to standard alpha blending after drawing the water region, ensuring subsequent draw calls use normal
            // transparency instead of the additive blend mode used for water effects.
            stage.batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        }
        stage.batch.shader = null
        stage.batch.end()
    }

    /**
     * Computes the sub-region of the FBO texture (in texel coordinates) that corresponds
     * to a given rectangle in world space.
     *
     * The function takes into account:
     * - the current camera world rect
     * - the viewport content box inside the FBO (in case of letterboxing)
     * - proper rounding (floor/ceil) to avoid off-by-one gaps
     *
     * @param rectWorldX the x-coordinate of the world-space rectangle (bottom-left corner)
     * @param rectWorldY the y-coordinate of the world-space rectangle (bottom-left corner)
     * @param rectWorldWidth the width of the rectangle in world units
     * @param rectWorldHeight the height of the rectangle in world units
     * @param camera the orthographic camera currently rendering the scene
     * @param textureWidth the full width of the FBO texture in pixels
     * @param textureHeight the full height of the FBO texture in pixels
     *
     * @return [TextureRegionRect] describing the subregion inside the FBO texture
     *         (texel coordinates, bottom-origin). When drawing this region with a Batch,
     *         set `flipY = true` to account for the FBO texture inversion.
     */
    private fun worldRectToTexRegion(
        rectWorldX: Float,
        rectWorldY: Float,
        rectWorldWidth: Float,
        rectWorldHeight: Float,
        camera: OrthographicCamera,
        textureWidth: Int,
        textureHeight: Int,
    ): TextureRegionRect {
        // 1. Camera world rect
        val cameraWorldWidth = camera.viewportWidth * camera.zoom
        val cameraWorldHeight = camera.viewportHeight * camera.zoom
        val cameraWorldX = camera.position.x - cameraWorldWidth * 0.5f
        val cameraWorldY = camera.position.y - cameraWorldHeight * 0.5f

        // 2. Normalize world rect to [0..1] within the camera world rect
        val normalizedU0 = ((rectWorldX - cameraWorldX) / cameraWorldWidth).coerceIn(0f, 1f)
        val normalizedV0 = ((rectWorldY - cameraWorldY) / cameraWorldHeight).coerceIn(0f, 1f)
        val normalizedU1 = (((rectWorldX + rectWorldWidth) - cameraWorldX) / cameraWorldWidth).coerceIn(0f, 1f)
        val normalizedV1 = (((rectWorldY + rectWorldHeight) - cameraWorldY) / cameraWorldHeight).coerceIn(0f, 1f)

        // 3. Map normalized coordinates into the viewport's content box inside the FBO
        val viewport = stage.viewport
        val contentWidthInPixels = viewport.screenWidth
        val contentHeightInPixels = viewport.screenHeight
        val contentOffsetX = (textureWidth - contentWidthInPixels) * 0.5f
        val contentOffsetY = (textureHeight - contentHeightInPixels) * 0.5f

        // 4. Convert to texel coordinates (bottom-origin)
        val sourceX0 = kotlin.math.floor(contentOffsetX + normalizedU0 * contentWidthInPixels).toInt()
        val sourceY0 = kotlin.math.floor(contentOffsetY + normalizedV0 * contentHeightInPixels).toInt()
        val sourceX1 = kotlin.math.ceil(contentOffsetX + normalizedU1 * contentWidthInPixels).toInt()
        val sourceY1 = kotlin.math.ceil(contentOffsetY + normalizedV1 * contentHeightInPixels).toInt()

        // 5. Compute width and height of the region
        val sourceWidth = (sourceX1 - sourceX0).coerceAtLeast(1)
        val sourceHeight = (sourceY1 - sourceY0).coerceAtLeast(1)

        return TextureRegionRect(
            sourceX = sourceX0.coerceAtLeast(0),
            sourceY = sourceY0.coerceAtLeast(0),
            sourceWidth = sourceWidth,
            sourceHeight = sourceHeight,
        )
    }

    private data class TextureRegionRect(
        val sourceX: Int,
        val sourceY: Int,
        val sourceWidth: Int,
        val sourceHeight: Int,
    )
}
