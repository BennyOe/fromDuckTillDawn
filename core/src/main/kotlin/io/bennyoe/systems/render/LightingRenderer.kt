package io.bennyoe.systems.render

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer
import com.badlogic.gdx.maps.tiled.tiles.AnimatedTiledMapTile
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.github.quillraven.fleks.Family
import com.github.quillraven.fleks.World
import io.bennyoe.components.ChainRenderComponent
import io.bennyoe.components.GameStateComponent
import io.bennyoe.components.ImageComponent
import io.bennyoe.components.ParallaxComponent
import io.bennyoe.components.ParticleType
import io.bennyoe.components.ShaderRenderingComponent
import io.bennyoe.components.TiledTextureComponent
import io.bennyoe.components.TimeOfDay
import io.bennyoe.components.TransformComponent
import io.bennyoe.components.WaterComponent
import io.bennyoe.components.animation.AnimationComponent
import io.bennyoe.config.GameConstants.UNIT_SCALE
import io.bennyoe.lightEngine.core.Scene2dLightEngine
import io.bennyoe.systems.render.DrawUtils.drawRegion
import kotlin.math.atan2

private fun floorMod(
    x: Float,
    m: Float,
): Float = ((x % m) + m) % m

class LightingRenderer(
    val stage: Stage,
    val world: World,
    val lightEngine: Scene2dLightEngine,
    val mapRenderer: OrthogonalTiledMapRenderer,
    val shaderService: ShaderService,
) {
    private val shapeRenderer: ShapeRenderer = ShapeRenderer()
    private val waterFamily by lazy { world.family { all(WaterComponent, TransformComponent) } }
    private val tmpVec = Vector2()

    fun render(
        renderQueue: List<RenderableElement>,
        playerActor: Actor,
        orthoCam: OrthographicCamera,
        gameStateCmp: GameStateComponent,
        continuousTime: Float,
        rainMaskFamily: Family,
        chainFamily: Family,
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
                            val parallaxCmp = with(world) { renderable.entity.getOrNull(ParallaxComponent) } // ADDED Check
                            val animationCmp = with(world) { renderable.entity.getOrNull(AnimationComponent) } // ADDED Check
                            if (parallaxCmp != null && animationCmp == null) {
                                shaderService.switchToDefaultIfNeeded(engine, currentShader)
                                drawTilingParallaxBackground(engine, renderable.imageCmp, renderable.transformCmp, parallaxCmp)
                                ShaderType.DEFAULT
                            } else {
                                renderEntityWithCorrectShader(engine, renderable, currentShader, gameStateCmp.timeOfDay, continuousTime)
                            }
                        }

                        is RenderableElement.EntityWithParticle -> {
                            shaderService.switchToDefaultIfNeeded(engine, currentShader)
                            if (!renderable.particleCmp.enabled) return@forEach

                            renderable.particleCmp.actor.setPosition(
                                renderable.transformCmp.position.x,
                                renderable.transformCmp.position.y,
                            )
                            when (renderable.particleCmp.type) {
                                ParticleType.RAIN -> drawParticleWithStencilMask(engine, renderable, rainMaskFamily, false)
                                ParticleType.AIR_BUBBLES -> drawParticleWithStencilMask(engine, renderable, waterFamily, true)
                                else -> renderable.particleCmp.actor.draw(engine.batch, 1f)
                            }
                            ShaderType.DEFAULT
                        }
                    }
            }
            renderChains(engine, chainFamily)
        }
    }

    private fun drawTilingParallaxBackground(
        engine: Scene2dLightEngine,
        imageCmp: ImageComponent,
        transformCmp: TransformComponent,
        parallaxCmp: ParallaxComponent,
    ) {
        // Resolve drawable/region/texture; abort early on missing data
        val drawable = imageCmp.image.drawable as? TextureRegionDrawable ?: return
        val region = drawable.region
        val texture = region.texture ?: return

        // Ensure correct wrap: horizontal repeat (U), vertical clamp (V)
        if (texture.uWrap != Texture.TextureWrap.Repeat || texture.vWrap != Texture.TextureWrap.ClampToEdge) {
            texture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.ClampToEdge)
        }

        // Basic sanity checks for source (pixels) and destination (world units)
        val texturePixelWidth = region.regionWidth.toFloat()
        val texturePixelHeight = region.regionHeight.toFloat()
        if (texturePixelWidth <= 0f || texturePixelHeight <= 0f) return
        if (transformCmp.width <= 0f || transformCmp.height <= 0f) return

        // Destination rectangle in world coordinates
        val destX = parallaxCmp.initialPosition.x
        val destWidth = parallaxCmp.worldWidth
        val destY = parallaxCmp.initialPosition.y
        val destHeight = transformCmp.height

        // Use imageCmp.image.width as the base tile width in world units.
        val tileWidthWu = imageCmp.image.width
        if (tileWidthWu <= 0.0f) return

        // Calculate total offset in World Units
        val offsetWu = destX - transformCmp.position.x

        // Convert world offset to "tile" offset
        val offsetTiles = offsetWu / tileWidthWu

        // Get the fractional part of the tile offset (e.g., 0.25 for 25% into a tile)
        val phaseTiles = floorMod(offsetTiles, 1.0f)

        // Calculate how many tiles are visible in the destination rectangle
        val tilesVisible = destWidth / tileWidthWu

        // Get the U-coordinate span of the texture region
        val tileSpanU = region.u2 - region.u

        // Calculate start and end U-coordinates
        val u0 = region.u + phaseTiles * tileSpanU
        val u1 = u0 + tilesVisible * tileSpanU

        // Apply flip if necessary
        val (uStart, uEnd) = if (!imageCmp.flipImage) u0 to u1 else u1 to u0

        // Draw using UVs (U repeats horizontally, V covers the full region vertically)
        engine.batch.draw(
            region.texture,
            destX,
            destY,
            destWidth,
            destHeight,
            uStart,
            region.v2,
            uEnd,
            region.v,
        )
    }

    private fun renderChains(
        engine: Scene2dLightEngine,
        ropeFamily: Family,
    ) {
        // Ropes don't need normal mapping, so ensure default shader
        shaderService.switchToDefaultIfNeeded(engine, ShaderType.CUSTOM)

        ropeFamily.forEach { entity ->
            val ropeCmp = entity[ChainRenderComponent]
            val region = ropeCmp.texture
            val segmentHeight = ropeCmp.segmentHeight
            val segmentWidth = region.regionWidth * UNIT_SCALE

            val p1 = ropeCmp.bodyA.getWorldPoint(ropeCmp.joint.localAnchorA)
            val p2 = ropeCmp.bodyB.getWorldPoint(ropeCmp.joint.localAnchorB)

            val direction = tmpVec.set(p2).sub(p1)
            val distance = direction.len()
            val angleRad = atan2(direction.y, direction.x)
            val angleDeg = angleRad * MathUtils.radDeg
            direction.nor()

            val numSegments = MathUtils.ceil(distance / segmentHeight)
            for (i in 0 until numSegments) {
                val segmentStart = i * segmentHeight
                val centerX = p1.x + direction.x * (segmentStart + segmentHeight / 2f)
                val centerY = p1.y + direction.y * (segmentStart + segmentHeight / 2f)

                val originX = segmentWidth / 2f
                val originY = segmentHeight / 2f

                val drawX = centerX - originX
                val drawY = centerY - originY

                engine.batch.draw(
                    region,
                    drawX,
                    drawY,
                    originX,
                    originY,
                    segmentWidth,
                    segmentHeight,
                    1f,
                    1f,
                    angleDeg - 90f,
                )
            }
        }
    }

    private fun drawParticleWithStencilMask(
        engine: Scene2dLightEngine,
        renderable: RenderableElement.EntityWithParticle,
        maskFamily: Family,
        drawOnlyInsideMask: Boolean,
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
        maskFamily.forEach { mask ->
            val transformCmp = mask[TransformComponent]
            shapeRenderer.rect(transformCmp.position.x, transformCmp.position.y, transformCmp.width, transformCmp.height)
        }
        shapeRenderer.end()

        // Re-enable writing to the color buffer for the actual rain
        Gdx.gl.glColorMask(true, true, true, true)

        if (drawOnlyInsideMask) {
            Gdx.gl.glStencilFunc(GL20.GL_EQUAL, 1, 0xFF)
        } else {
            // Now, change the rule: only draw where the stencil value is NOT 1
            Gdx.gl.glStencilFunc(GL20.GL_NOTEQUAL, 1, 0xFF)
        }

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

                if (renderable.tiledCmp != null) {
                    renderWithTiledNormalMapping(engine, renderable.imageCmp, renderable.shaderRenderingCmp, renderable.tiledCmp)
                } else {
                    renderWithNormalMapping(engine, renderable.imageCmp, renderable.shaderRenderingCmp)
                }

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

        return newShaderType
    }

    private fun renderWithTiledNormalMapping(
        engine: Scene2dLightEngine,
        imageCmp: ImageComponent,
        shaderCmp: ShaderRenderingComponent,
        tiledCmp: TiledTextureComponent,
    ) {
        if (shaderCmp.specular != null) {
            engine.drawTiled(
                diffuse = shaderCmp.diffuse!!,
                normals = shaderCmp.normal!!,
                specular = shaderCmp.specular!!,
                x = imageCmp.image.x,
                y = imageCmp.image.y,
                width = imageCmp.image.width,
                height = imageCmp.image.height,
                scale = tiledCmp.scale,
                unitScale = UNIT_SCALE,
            )
        } else {
            engine.drawTiled(
                diffuse = shaderCmp.diffuse!!,
                normals = shaderCmp.normal!!,
                x = imageCmp.image.x,
                y = imageCmp.image.y,
                width = imageCmp.image.width,
                height = imageCmp.image.height,
                scale = tiledCmp.scale,
                unitScale = UNIT_SCALE,
            )
        }
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
