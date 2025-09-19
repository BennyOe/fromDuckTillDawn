package io.bennyoe.systems.render

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer
import com.badlogic.gdx.maps.tiled.tiles.AnimatedTiledMapTile
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.github.quillraven.fleks.Family
import io.bennyoe.components.GameStateComponent
import io.bennyoe.components.ImageComponent
import io.bennyoe.components.ParticleType
import io.bennyoe.components.RainMaskComponent
import io.bennyoe.components.ShaderRenderingComponent
import io.bennyoe.components.TimeOfDay
import io.bennyoe.lightEngine.core.Scene2dLightEngine
import io.bennyoe.systems.render.DrawUtils.drawRegion

class LightingRenderer(
    val stage: Stage,
    val lightEngine: Scene2dLightEngine,
    val mapRenderer: OrthogonalTiledMapRenderer,
    val shaderService: ShaderService,
) {
    private val shapeRenderer: ShapeRenderer = ShapeRenderer()

    fun render(
        renderQueue: List<RenderableElement>,
        playerActor: Actor,
        orthoCam: OrthographicCamera,
        gameStateCmp: GameStateComponent,
        continuousTime: Float,
        rainMaskFamily: Family,
    ) {
        AnimatedTiledMapTile.updateAnimationBaseTime()
        mapRenderer.setView(orthoCam)

        lightEngine.renderLights(playerActor) { engine ->
            var currentShader = ShaderType.NONE
            engine.batch.projectionMatrix = orthoCam.combined

            renderQueue.forEach { renderable ->
                currentShader =
                    when (renderable) {
                        is RenderableElement.TileLayer -> {
                            shaderService.switchToDefaultIfNeeded(engine, currentShader)
                            mapRenderer.renderTileLayer(renderable.layer)
                            ShaderType.DEFAULT
                        }

                        is RenderableElement.ImageLayer -> {
                            // tint the farest background at night dark
                            // TODO this has to be generic
                            if (gameStateCmp.getTimeOfDay() == TimeOfDay.NIGHT && renderable.layer.name == "backgroundMountain") {
                                mapRenderer.batch.color = Color(0.6f, 0.6f, 0.6f, 1f)
                            } else {
                                mapRenderer.batch.color = Color.WHITE
                            }

                            shaderService.switchToDefaultIfNeeded(engine, currentShader)
                            mapRenderer.renderImageLayer(renderable.layer)
                            ShaderType.DEFAULT
                        }

                        is RenderableElement.EntityWithImage -> {
                            renderEntityWithCorrectShader(engine, renderable, currentShader, gameStateCmp.timeOfDay, continuousTime)
                        }

                        is RenderableElement.EntityWithParticle -> {
                            shaderService.switchToDefaultIfNeeded(engine, currentShader)
                            if (!renderable.particleCmp.enabled) return@forEach

                            if (renderable.particleCmp.type == ParticleType.RAIN) {
                                drawParticleWithStencilMask(engine, renderable, rainMaskFamily)
                            } else {
                                renderable.particleCmp.actor.draw(engine.batch, 1f)
                            }
                            ShaderType.DEFAULT
                        }
                    }
            }
        }
    }

    private fun drawParticleWithStencilMask(
        engine: Scene2dLightEngine,
        renderable: RenderableElement.EntityWithParticle,
        rainMaskFamily: Family,
    ) {
        engine.batch.flush()
        engine.batch.end()
        Gdx.gl.glEnable(GL20.GL_STENCIL_TEST)
        Gdx.gl.glClear(GL20.GL_STENCIL_BUFFER_BIT)

        // Draw the mask
        // Don't write to the color buffer, only to the stencil buffer
        Gdx.gl.glColorMask(false, false, false, false)

        // Always pass the stencil test and set the stencil buffer value to 1 where drawing occurs
        Gdx.gl.glStencilFunc(GL20.GL_ALWAYS, 1, 0xFF)

        // GL_REPLACE: Replace the stencil buffer's value with the reference value (1).
        Gdx.gl.glStencilOp(GL20.GL_REPLACE, GL20.GL_REPLACE, GL20.GL_REPLACE)

        // Now, use the ShapeRenderer to draw the shapes from Tiled
        shapeRenderer.projectionMatrix = stage.camera.combined
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        rainMaskFamily.forEach { rainMask ->
            val rainMaskCmp = rainMask[RainMaskComponent]
            shapeRenderer.rect(rainMaskCmp.x, rainMaskCmp.y, rainMaskCmp.width, rainMaskCmp.height)
        }
        shapeRenderer.end()

        // Re-enable writing to the color buffer for the actual rain
        Gdx.gl.glColorMask(true, true, true, true)

        // Now, change the rule: only draw where the stencil value is NOT 1
        Gdx.gl.glStencilFunc(GL20.GL_NOTEQUAL, 1, 0xFF)
        // And make sure the rain itself doesn't change the stencil buffer
        Gdx.gl.glStencilOp(GL20.GL_KEEP, GL20.GL_KEEP, GL20.GL_KEEP)

        // Draw the rain effect
        engine.batch.begin()
        renderable.particleCmp.actor.draw(engine.batch, 1f)
        engine.batch.end()

        Gdx.gl.glDisable(GL20.GL_STENCIL_TEST)
        engine.batch.begin()
    }

    private fun renderEntityWithCorrectShader(
        engine: Scene2dLightEngine,
        renderable: RenderableElement.EntityWithImage,
        currentShader: ShaderType,
        timeOfDay: Float,
        continuousTime: Float,
    ): ShaderType {
        val newShaderType =
            if (renderable.shaderRenderingCmp?.normal != null) {
                // Entity needs lighting shader
                val updatedShader = shaderService.switchToLightingIfNeeded(engine, currentShader)

                // if entity is hit -> apply hitStop color overlay
                val hadHit = renderable.hitEffectComponent != null
                if (hadHit) {
                    val it = renderable.hitEffectComponent
                    val strength = 1f - (it.timer / it.duration)
                    engine.setOverlayColor(it.color, strength)
                }

                renderWithNormalMapping(engine, renderable.imageCmp, renderable.shaderRenderingCmp)

                if (hadHit) {
                    engine.resetOverlayColor()
                }
                updatedShader
            } else if (renderable.shaderRenderingCmp?.shader != null) {
                // entity needs no lighting-shader but has custom shader (sun, moon etc.)
                val desiredShader = renderable.shaderRenderingCmp.shader!!
                if (currentShader != ShaderType.CUSTOM || engine.batch.shader != desiredShader) {
                    shaderService.switchToCustom(engine, desiredShader)
                }
                shaderService.updateUniformsPerFrame(
                    renderable.shaderRenderingCmp.shader!!,
                    renderable.shaderRenderingCmp.uniforms,
                    timeOfDay,
                    continuousTime,
                )

                // if shaderCmp has a noise texture, use it
                shaderService.configureNoiseIfPresent(renderable.shaderRenderingCmp)

                // pass the correct texture coordinates to the shader (without this, the coordinates are the screen, not the texture coordinates)
                val drawable = renderable.imageCmp.image.drawable as? TextureRegionDrawable
                if (drawable != null) {
                    val region = drawable.region
                    renderable.shaderRenderingCmp.shader?.setUniformf("u_texCoord_min", region.u, region.v)
                    renderable.shaderRenderingCmp.shader?.setUniformf("u_texCoord_max", region.u2, region.v2)
                }

                drawRegion(engine, renderable.imageCmp)
                ShaderType.CUSTOM
            } else {
                // Entity uses default shader
                val updatedShader = shaderService.switchToDefaultIfNeeded(engine, currentShader)
                drawRegion(engine, renderable.imageCmp)
                updatedShader
            }

        // sync the particle position
        renderable.particleCmp
            ?.actor
            ?.effect
            ?.setPosition(renderable.transformCmp.position.x, renderable.transformCmp.position.y)

        // Draw particles if present (use current shader)
        if (renderable.particleCmp != null && renderable.particleCmp.enabled) {
            renderable.particleCmp.actor.draw(engine.batch, 1f)
        }

        return newShaderType
    }

    private fun renderWithNormalMapping(
        engine: Scene2dLightEngine,
        imageCmp: ImageComponent,
        shaderCmp: ShaderRenderingComponent,
    ) {
        if (shaderCmp.specular != null) {
            engine.draw(
                diffuse = shaderCmp.diffuse!!,
                normals = shaderCmp.normal!!,
                specular = shaderCmp.specular!!,
                x = imageCmp.image.x,
                y = imageCmp.image.y,
                width = imageCmp.image.width,
                height = imageCmp.image.height,
                flipX = imageCmp.flipImage,
            )
        } else {
            engine.draw(
                diffuse = shaderCmp.diffuse!!,
                normals = shaderCmp.normal!!,
                x = imageCmp.image.x,
                y = imageCmp.image.y,
                width = imageCmp.image.width,
                height = imageCmp.image.height,
                flipX = imageCmp.flipImage,
            )
        }
    }
}
