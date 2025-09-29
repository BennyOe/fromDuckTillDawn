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

private const val WATER_TRANSPARENCY = 0.35f

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

            // Bind the offscreen scene (FBO) texture to texture unit 1.
            // The shader will sample this as the "background" that gets refracted.
            fboTexture.bind(1)

            // Switch the active unit back to 0 so the PolygonSpriteBatch can bind the water texture
            Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0)

            // Install and bind the custom shader that mixes water + background with distortion.
            polygonSpriteBatch.shader = waterCmp.shader
            waterCmp.shader?.bind()

            // Tell the shader which texture unit each sampler corresponds to:
            //  - unit 0: water texture (atlas region)
            //  - unit 1: FBO texture (offscreen-rendered scene)
            waterCmp.shader?.setUniformi("u_texture", 0)
            waterCmp.shader?.setUniformi("u_fbo_texture", 1)

            letterBoxAndHiDpiMapping(fboTexture, waterCmp)

            // Per-frame uniforms for the distortion (time, speeds, intensity, etc.)
            shaderService.updateUniformsPerFrame(
                shader = waterCmp.shader!!,
                uniforms = waterCmp.uniforms,
                continuousTime = continuousTime,
            )

            // Ensure standard compositing: background (FBO) mixed with water texture by sprite alpha.
            polygonSpriteBatch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)

            // Draw the wave geometry. The batch will send the vertices, texture coordinates,
            // and color (including alpha) to the shader, which handles the final composition.
            drawWaves(waterCmp, orthoCam)

            // Reset the shader on the batch to `null` to prevent it from affecting
            // subsequent, unrelated draw calls in the main render loop.
            polygonSpriteBatch.shader = null
        }
    }

    /**
     * Calculates and sets shader uniforms for letterboxing and HiDPI-aware mapping.
     *
     * This method determines the position and size of the actual viewport content within the FBO texture,
     * accounting for HiDPI displays and letterboxing (e.g., black bars from Fit/ExtendViewport).
     * It computes the content offset and size in FBO texels and passes them as uniforms to the shader.
     *
     * @param fboTexture The Framebuffer Object texture containing the rendered scene.
     * @param waterCmp The WaterComponent whose shader will receive the uniforms.
     */
    private fun letterBoxAndHiDpiMapping(
        fboTexture: Texture,
        waterCmp: WaterComponent,
    ) {
        val vp = stage.viewport
        val fboTexW = fboTexture.width.toFloat() // FBO size in *texels* (physical pixels)
        val fboTexH = fboTexture.height.toFloat()

        // Logical window size (maybe half the FBO on HiDPI / Retina)
        val winLogicalW = Gdx.graphics.width.toFloat()
        val winLogicalH = Gdx.graphics.height.toFloat()

        // Content box as fractions of the logical window (0..1)
        val contentRelX = vp.screenX / winLogicalW
        val contentRelY = vp.screenY / winLogicalH
        val contentRelW = vp.screenWidth / winLogicalW
        val contentRelH = vp.screenHeight / winLogicalH

        // Convert those fractions to *FBO texels* (works for both FBO=Backbuffer and FBO=Viewport)
        val contentOffsetXInFbo = contentRelX * fboTexW
        val contentOffsetYInFbo = contentRelY * fboTexH
        val contentWidthInFbo = contentRelW * fboTexW
        val contentHeightInFbo = contentRelH * fboTexH

        // Pass letterbox info to the shader:
        //  - u_fboSize:            full FBO texture size (texels)
        //  - u_contentOffset:      bottom-left of the viewport content inside the FBO (texels)
        //  - u_contentSize:        size of the viewport content inside the FBO (texels)
        waterCmp.shader!!.setUniformf("u_fboSize", fboTexW, fboTexH)
        waterCmp.shader!!.setUniformf("u_contentOffset", contentOffsetXInFbo, contentOffsetYInFbo)
        waterCmp.shader!!.setUniformf("u_contentSize", contentWidthInFbo, contentHeightInFbo)
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

        ensureStripMesh(waterCmp) // build once or when column count changed
        updateStripTopY(waterCmp) // just mutate the top y's
        waterCmp.waterSprite!!.setRegion(waterCmp.waterRegion)
        waterCmp.waterSprite!!.draw(polygonSpriteBatch)

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
        val cols = waterCmp.columns
        val n = cols.size
        if (n <= 1) return

        waterCmp.ensureDeltaCapacity(n)
        val l = waterCmp.lDeltas
        val r = waterCmp.rDeltas

        // --- Part 1: per-column spring update ---
        for (i in 0 until n) {
            val c = cols[i]
            c.update(waterCmp.dampening, waterCmp.tension)
        }

        // --- Part 2: wave spreading ---
        val s = waterCmp.spread
        val passes = waterCmp.spreadPasses

        repeat(passes) {
            val last = n - 1
            for (i in 0 until last) {
                val left = cols[i]
                val right = cols[i + 1]
                val d = s * (right.height - left.height)
                l[i + 1] = d
                r[i] = -d
                left.speed += d
                right.speed -= d
            }

            for (i in 0 until last) {
                val dl = l[i + 1]
                val dr = r[i]
                cols[i].height += dl
                cols[i + 1].height += dr
            }
        }
    }

    // initialization
    private fun ensureStripMesh(waterCmp: WaterComponent) {
        val cols = waterCmp.columns
        val n = cols.size
        if (n < 2) return

        if (waterCmp.waterVertices != null && waterCmp.meshCapacity == n) return

        // Build vertex loop: top edge (0..n-1), then bottom edge (n-1..0)
        // Each vertex: (x, y) -> 2 floats
        val vertexCount = n * 2 // top + bottom
        val vertices = FloatArray(vertexCount * 2)

        // Fill top edge (x, height)
        var vi = 0
        for (i in 0 until n) {
            val c = cols[i]
            vertices[vi++] = c.x
            vertices[vi++] = c.height
        }
        // Fill bottom edge reversed (x, y)
        for (i in n - 1 downTo 0) {
            val c = cols[i]
            vertices[vi++] = c.x
            vertices[vi++] = c.y
        }

        // Triangulate once for the closed polygon
        val tris = triangulator.computeTriangles(vertices).toArray()

        val region = PolygonRegion(waterTextureRegion, vertices, tris)
        waterCmp.waterRegion = region
        waterCmp.waterVertices = region.vertices
        waterCmp.waterSprite =
            PolygonSprite(region).apply {
                setColor(1f, 1f, 1f, WATER_TRANSPARENCY)
            }
        waterCmp.meshCapacity = n
    }

    private fun updateStripTopY(waterCmp: WaterComponent) {
        val cols = waterCmp.columns
        val n = cols.size
        val vertices = waterCmp.waterRegion?.vertices ?: return

        var vi = 1
        for (i in 0 until n) {
            vertices[vi] = cols[i].height
            vi += 2
        }
    }
}
