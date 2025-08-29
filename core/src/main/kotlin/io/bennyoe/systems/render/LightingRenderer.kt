package io.bennyoe.systems.render

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer
import com.badlogic.gdx.maps.tiled.tiles.AnimatedTiledMapTile
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import io.bennyoe.components.GameStateComponent
import io.bennyoe.components.ImageComponent
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
    fun render(
        renderQueue: List<RenderableElement>,
        playerActor: Actor,
        orthoCam: OrthographicCamera,
        gameStateCmp: GameStateComponent,
        continuousTime: Float,
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
                            renderable.particleCmp.actor.draw(engine.batch, 1f)
                            ShaderType.DEFAULT
                        }
                    }
            }
        }
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
                renderWithNormalMapping(engine, renderable.imageCmp, renderable.shaderRenderingCmp)
                updatedShader
            } else if (renderable.shaderRenderingCmp?.shader != null) {
                val desiredShader = renderable.shaderRenderingCmp.shader!!
                if (currentShader != ShaderType.CUSTOM || engine.batch.shader != desiredShader) {
                    shaderService.switchToCustom(engine, desiredShader)
                }
                shaderService.updateUniformsPerFrame(renderable.shaderRenderingCmp, timeOfDay, continuousTime)

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

        // Draw particles if present (use current shader)
        renderable.particleCmp?.actor?.draw(engine.batch, 1f)

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
