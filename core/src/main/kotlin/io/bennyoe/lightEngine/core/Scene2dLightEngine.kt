package com.github.bennyOe.core

import box2dLight.RayHandler
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.utils.viewport.Viewport
import com.github.bennyOe.scene2d.NormalMappedActor

class Scene2dLightEngine(
    rayHandler: RayHandler,
    cam: OrthographicCamera,
    batch: SpriteBatch,
    viewport: Viewport,
    val stage: Stage?,
    useDiffuseLight: Boolean = true,
    maxShaderLights: Int = 20,
) : AbstractLightEngine(rayHandler, cam, batch, viewport, useDiffuseLight, maxShaderLights) {
    /**
     * Performs the complete lighting render pass using normal mapping and Box2D shadows.
     *
     * This function sets up the shader, uploads all light properties, invokes a user-provided lambda to render the
     * scene with lighting, and then renders Box2D-based shadows on top. It must be called once per frame.
     *
     * ### What this function does:
     * - Configures the batch with the shader and camera matrix.
     * - Applies lighting-related uniforms to the shader (light count, color, falloff, direction, etc.).
     * - Calls the [drawScene] lambda where you render all visible objects using your own draw logic.
     * - Renders Box2D shadows via [RayHandler].
     *
     * ### Requirements inside [drawScene]:
     * - **Normal map must be bound to texture unit 1** before calling `batch.draw(...)`.
     * - **Diffuse texture must be bound to texture unit 0** before calling `batch.draw(...)`.
     * - Use the batch normally for rendering your sprites — lighting will be automatically applied by the shader.
     *
     * @param drawScene Lambda in which your game scene should be rendered with lighting applied.
     */
    fun renderLights(drawScene: (Scene2dLightEngine) -> Unit) {
        batch.projectionMatrix = cam.combined
        viewport.apply()

        applyShaderUniforms()

        batch.begin()
        drawScene(this)
        batch.end()

        // Setze den Shader auf den Standard zurück, damit andere Systeme nicht gestört werden
        batch.shader = null

        // Zeichne die Schatten darüber
        rayHandler.setCombinedMatrix(cam)
        rayHandler.updateAndRender()
    }

    /**
     * Draws a Scene2D [Actor] with optional normal mapping.
     *
     * If the actor is a [NormalMappedActor], it binds the normal map to texture unit 1
     * and the diffuse texture to unit 0, and renders it with lighting effects applied.
     * If the actor is not normal-mapped, it disables normal mapping in the shader and
     * renders the actor using the standard [Actor.draw] method.
     *
     * This method must be called **within** the [renderLights] lambda to ensure lighting
     * uniforms and shader context are active.
     *
     * @param actor The [Actor] to render. May or may not have an associated normal map.
     */
    fun draw(actor: Actor) {
        // Fall 1: Ein spezieller Actor mit Normal-Map
        if (actor is NormalMappedActor) {
            // Stelle sicher, dass unser Beleuchtungs-Shader aktiv ist
            if (batch.shader != this.shader) batch.shader = this.shader

            // Sage dem Shader, dass er die Normal-Map verwenden soll
            batch.shader.bind()
            shader.setUniformi("u_useNormalMap", 1)

            // Binde die Texturen an die korrekten Einheiten
            actor.normalMapTexture?.bind(1)
            actor.specularTexture?.bind(2)
            actor.diffuseTexture.bind(0)

            // KORREKTER AUFRUF: Diese `draw`-Methode akzeptiert eine `Texture`.
            // Sie ignoriert zwar die `scale`- und `rotation`-Eigenschaften des Actors direkt,
            // aber da deine Spielfiguren keine NormalMappedActors sind, ist das für sie nicht relevant.
            batch.draw(actor.diffuseTexture, actor.x, actor.y, actor.width, actor.height)
        }
        // Fall 2: Alle anderen normalen Actors (Player, Mushroom, etc.)
        else {
            // Stelle sicher, dass der Standard-Shader aktiv ist
            if (batch.shader != null) batch.shader = null // TODO not needed

            // Überlasse dem Actor das Zeichnen! Er kennt seine Größe, Skalierung und Drehung.
            // Diese Methode funktioniert für deine Spielfiguren und behebt das Größen- und Spiegelungsproblem.
            if (lastNormalMap != null) batch.flush()
            if (lastSpecularMap != null) batch.flush()
            batch.shader.bind()
            shader.setUniformi("u_useNormalMap", 0)
            shader.setUniformi("u_useSpecularMap", 0)
            actor.draw(batch, 1.0f)
            lastNormalMap = null
            lastSpecularMap = null
        }
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
