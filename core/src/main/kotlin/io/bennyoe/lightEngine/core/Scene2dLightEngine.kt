package io.bennyoe.lightEngine.core

import box2dLight.RayHandler
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.utils.viewport.Viewport

class Scene2dLightEngine(
    rayHandler: RayHandler,
    cam: OrthographicCamera,
    batch: SpriteBatch,
    viewport: Viewport,
    val stage: Stage?,
    useDiffuseLight: Boolean = true,
    maxShaderLights: Int = 20,
) : AbstractLightEngine(rayHandler, cam, batch, viewport, useDiffuseLight, maxShaderLights) {
    fun renderLights(drawScene: (Scene2dLightEngine) -> Unit) {
        batch.projectionMatrix = cam.combined
        viewport.apply()

        // KORREKTUR: Shader wird hier ZENTRAL für den Batch gesetzt.
        setShaderToEngineShader()
        applyShaderUniforms()

        batch.begin()
        drawScene(this)
        batch.end()

        // Shader zurücksetzen, damit andere Rendering-Vorgänge nicht gestört werden.
        setShaderToDefaultShader()

        // Schatten darüber zeichnen.
        rayHandler.setCombinedMatrix(cam)
        rayHandler.updateAndRender()
    }

    fun draw(
        diffuse: TextureRegion,
        normals: TextureRegion,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        flipX: Boolean = false,
    ) {
        // Dem Shader mitteilen, dass eine Normal-Map verwendet wird.
        shader.setUniformi("u_useNormalMap", 1)
        shader.setUniformi("u_useSpecularMap", 0)

        // Texturen an die korrekten Einheiten binden.
        normals.texture.bind(1) // Normal-Map an Einheit 1
        diffuse.texture.bind(0) // Diffuse-Map an Einheit 0

        if (flipX) {
            batch.draw(diffuse, x + width, y, -width, height)
        } else {
            batch.draw(diffuse, x, y, width, height)
        }
    }

    /**
     * Draws a standard Scene2D Image without normal mapping. This method manages the shader state and flushes the batch if needed.
     */
    fun drawWithoutNormalMap(image: Image) {
        // If we were just drawing with a normal map, flush the batch before changing the shader state.
        if (lastNormalMap != null) {
            batch.flush()
        }

        // Set our lighting shader and tell it to ignore the normal map uniform.
        batch.shader = this.shader
        shader.bind()
        shader.setUniformi("u_useNormalMap", 0)

        // Draw the image normally
        image.draw(batch, 1f)

        // Update the state to reflect that we are no longer using a normal map.
        lastNormalMap = null
    }

    override fun resize(
        width: Int,
        height: Int,
    ) {
        if (stage == null) return
        stage.viewport.update(width, height, true)
        super.resize(width, height)
    }
}
