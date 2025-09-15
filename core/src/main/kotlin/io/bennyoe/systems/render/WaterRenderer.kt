package io.bennyoe.systems.render

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.PolygonRegion
import com.badlogic.gdx.graphics.g2d.PolygonSprite
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.EarClippingTriangulator
import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.quillraven.fleks.Family
import io.bennyoe.components.WaterComponent

/**
 * Handles the rendering of water entities.
 *
 * This class is responsible for two main tasks:
 * 1. Simulating the water surface physics (waves) based on `WaterColumn` data.
 * 2. Rendering the water geometry using a custom shader that combines a water texture
 * with a distorted background scene for a refractive effect.
 *
 * @param stage The main game stage, used for viewport calculations.
 * @param polygonSpriteBatch The batch used to render polygon-based geometry like the water surface.
 * @param worldObjectsAtlas The texture atlas containing the 'water' texture region.
 */
class WaterRenderer(
    val stage: Stage,
    val polygonSpriteBatch: PolygonSpriteBatch,
    val worldObjectsAtlas: TextureAtlas,
) {
    private val shaderService = ShaderService()
    private val triangulator = EarClippingTriangulator()
    private val waterTextureRegion: TextureRegion by lazy { worldObjectsAtlas.findRegion("water") }

    init {
        // Ensure linear filtering for the water texture to avoid pixelated waves.
        worldObjectsAtlas.apply { textures.forEach { it.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear) } }
    }

    /**
     * Renders all water entities for the current frame.
     *
     * This method performs a single, combined render pass for each water entity. It updates the
     * wave simulation, sets up the custom water shader with all necessary uniforms (including
     * textures and camera data), and then draws the dynamic water mesh.
     *
     * @param waterFamily A Fleks family containing all entities with a `WaterComponent`.
     * @param continuousTime A continuously increasing time value for animations.
     * @param fboTexture The texture from the Framebuffer Object containing the rendered game scene (the background).
     * @param orthoCam The game's primary orthographic camera.
     */
    fun render(
        waterFamily: Family,
        continuousTime: Float,
        fboTexture: Texture,
        orthoCam: OrthographicCamera,
    ) {
        stage.viewport.apply()

        waterFamily.forEach { e ->
            val waterCmp = e[WaterComponent]
            updateWaves(waterCmp)

            // --- Combined Render Pass ---

            // Step 1: Bind the FBO texture (the scene) to texture unit 1.
            // The shader will use this as the background to be distorted.
            fboTexture.bind(1)
            // Step 2: Reactivate texture unit 0. The PolygonSpriteBatch will automatically
            // bind the water's own texture to this unit when `sprite.draw()` is called.
            Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0)

            // Step 3: Set the custom shader on the batch and configure its uniforms.
            polygonSpriteBatch.shader = waterCmp.shader
            waterCmp.shader?.bind()

            // Pass texture unit indices to the shader.
            waterCmp.shader?.setUniformi("u_texture", 0)
            waterCmp.shader?.setUniformi("u_fbo_texture", 1)

            val vp = stage.viewport
            val texW = fboTexture.width
            val texH = fboTexture.height
            val contentW = vp.screenWidth
            val contentH = vp.screenHeight
            val offX = (texW - contentW) * 0.5f
            val offY = (texH - contentH) * 0.5f

            waterCmp.shader!!.setUniformf("u_fboSize", texW.toFloat(), texH.toFloat())
            waterCmp.shader!!.setUniformf("u_contentOffset", offX, offY)
            waterCmp.shader!!.setUniformf("u_contentSize", contentW.toFloat(), contentH.toFloat())

            // Update time-dependent and other custom uniforms for the distortion effect.
            shaderService.updateUniformsPerFrame(
                shader = waterCmp.shader!!,
                uniforms = waterCmp.uniforms,
                continuousTime = continuousTime,
            )

            // Step 4: Ensure standard alpha blending is active. The shader will use the sprite's
            // alpha to mix the water and background colors.
            polygonSpriteBatch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)

            // Step 5: Draw the wave geometry. The batch will send the vertices, texture coordinates,
            // and color (including alpha) to the shader, which handles the final composition.
            drawWaves(waterCmp, orthoCam)

            // Step 6: Reset the shader on the batch to `null` to prevent it from affecting
            // subsequent, unrelated draw calls in the main render loop.
            polygonSpriteBatch.shader = null
        }
    }

    /**
     * Draws the dynamic water surface mesh.
     *
     * It iterates through the `WaterColumn` pairs, creating a quad for each segment of the
     * water surface. These quads are then triangulated and drawn as a `PolygonSprite`.
     *
     * @param waterCmp The `WaterComponent` containing the column data.
     * @param camera The camera used to set the batch's projection matrix.
     */
    private fun drawWaves(
        waterCmp: WaterComponent,
        camera: OrthographicCamera,
    ) {
        polygonSpriteBatch.projectionMatrix = camera.combined
        polygonSpriteBatch.begin()

        waterCmp.columns.zipWithNext().forEach { (c1, c2) ->
            // Define the four corners of the quad for this water segment.
            // The top vertices (c1.height, c2.height) are dynamic.
            val vertices =
                floatArrayOf(
                    c1.x,
                    c1.y, // Bottom-left
                    c1.x,
                    c1.height, // Top-left
                    c2.x,
                    c2.height, // Top-right
                    c2.x,
                    c2.y, // Bottom-right
                )

            // A quad is not a simple triangle, so we need to triangulate it.
            val triangles = triangulator.computeTriangles(vertices).toArray()
            val region = PolygonRegion(waterTextureRegion, vertices, triangles)
            val sprite = PolygonSprite(region)

            // Set the desired base transparency of the water.
            // The shader uses this alpha value to mix the water texture and the distorted background.
            sprite.setColor(1f, 1f, 1f, 0.25f)

            sprite.draw(polygonSpriteBatch)
        }
        polygonSpriteBatch.end()
    }

    /**
     * Simulates one step of the wave physics.
     *
     * This function first updates the height and speed of each column based on tension and
     * dampening. It then propagates wave energy between adjacent columns over several iterations
     * to create a smooth spreading effect.
     *
     * @param waterCmp The `WaterComponent` holding the state of the water columns.
     */
    private fun updateWaves(waterCmp: WaterComponent) {
        // Part 1: Apply spring physics to each individual column.
        waterCmp.columns.forEach { it.update(waterCmp.dampening, waterCmp.tension) }

        // Part 2: Spread the wave energy between neighboring columns.
        val lDeltas = FloatArray(waterCmp.columns.size)
        val rDeltas = FloatArray(waterCmp.columns.size)
        val s = waterCmp.spread

        // Run multiple passes for a more stable and fluid simulation.
        repeat(8) {
            // Calculate how much height/speed to transfer between neighbors.
            waterCmp.columns.zipWithNext().forEachIndexed { idx, (left, right) ->
                val d = s * (right.height - left.height)
                lDeltas[idx + 1] = d
                rDeltas[idx] = -d
                left.speed += d
                right.speed -= d
            }

            // Apply the calculated changes.
            waterCmp.columns.zipWithNext().forEachIndexed { idx, (left, right) ->
                left.height += lDeltas[idx + 1]
                right.height += rDeltas[idx]
            }
        }
    }
}
