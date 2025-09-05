package io.bennyoe.systems.render

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer
import com.badlogic.gdx.maps.tiled.tiles.AnimatedTiledMapTile
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import ktx.graphics.use

class SimpleRenderer(
    private val stage: Stage,
    private val mapRenderer: OrthogonalTiledMapRenderer,
    private val shaderService: ShaderService,
) {
    fun render(
        renderQueue: List<RenderableElement>,
        timeOfDay: Float,
        continuousTime: Float,
        orthoCam: OrthographicCamera,
    ) {
        // Simple rendering without lighting - everything in order
        AnimatedTiledMapTile.updateAnimationBaseTime()
        mapRenderer.setView(orthoCam)

        stage.batch.use(orthoCam.combined) {
            renderQueue.forEach { renderable ->
                when (renderable) {
                    is RenderableElement.TileLayer -> mapRenderer.renderTileLayer(renderable.layer)
                    is RenderableElement.ImageLayer -> mapRenderer.renderImageLayer(renderable.layer)
                    is RenderableElement.EntityWithImage -> {
                        if (renderable.shaderRenderingCmp?.shader != null) {
                            stage.batch.shader = renderable.shaderRenderingCmp.shader
                            shaderService.updateUniformsPerFrame(renderable.shaderRenderingCmp, timeOfDay, continuousTime)
                            shaderService.configureNoiseIfPresent(renderable.shaderRenderingCmp)
                        }
                        if (renderable.imageCmp.flipImage) {
                            // Draw flipped by adjusting position and using negative width
                            val texture = renderable.imageCmp.image.drawable as? TextureRegionDrawable
                            val region = texture?.region
                            if (region != null) {
                                val oldColor = it.color.cpy()
                                it.color = renderable.imageCmp.image.color
                                it.draw(
                                    region,
                                    renderable.imageCmp.image.x + renderable.imageCmp.image.width,
                                    renderable.imageCmp.image.y,
                                    -renderable.imageCmp.image.width,
                                    renderable.imageCmp.image.height,
                                )
                                it.color = oldColor
                            }
                        } else {
                            // Draw normally
                            renderable.imageCmp.image.draw(it, 1f)
                        }
                        stage.batch.shader = null
                    }

                    is RenderableElement.EntityWithParticle -> {
                        if (!renderable.particleCmp.enabled) return@forEach
                        renderable.particleCmp.actor.draw(it, 1f)
                    }
                }
            }
        }
    }
}
