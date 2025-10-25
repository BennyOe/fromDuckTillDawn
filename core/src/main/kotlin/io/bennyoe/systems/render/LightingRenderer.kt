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
import io.bennyoe.components.AnimationComponent
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
import io.bennyoe.config.GameConstants.UNIT_SCALE
import io.bennyoe.lightEngine.core.Scene2dLightEngine
import io.bennyoe.systems.render.DrawUtils.drawRegion
import kotlin.math.atan2
import kotlin.math.roundToInt

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
        val drawable = imageCmp.image.drawable as? TextureRegionDrawable ?: return
        val region = drawable.region
        val texture = region.texture ?: return

        // Ensure texture wrap modes are set correctly
        if (texture.uWrap != Texture.TextureWrap.Repeat || texture.vWrap != Texture.TextureWrap.ClampToEdge) {
            texture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.ClampToEdge)
        }

        // Texture dimensions in PIXELS
        val texturePixelWidth = region.regionWidth.toFloat()
        val texturePixelHeight = region.regionHeight.toFloat()
        if (texturePixelWidth <= 0 || texturePixelHeight <= 0) return

        if (transformCmp.width <= 0 || transformCmp.height <= 0) return

        // --- Destination Rectangle ---
        // X and Width are from the Tiled rectangle (defines the horizontal space)
        val destX = parallaxCmp.initialPosition.x
        val destWidth = parallaxCmp.worldWidth
        // Y is the bottom of the Tiled rectangle
        val destY = parallaxCmp.initialPosition.y
        // Height is the ORIGINAL image height to preserve aspect ratio
        val destHeight = transformCmp.height
        // --- End Destination Rectangle ---

        // --- srcX Calculation (Scrolling Offset in Pixels) ---
        val offsetWorldUnits = destX - transformCmp.position.x
        val offsetTexturePixels = (offsetWorldUnits / transformCmp.width) * texturePixelWidth
        var srcX = offsetTexturePixels % texturePixelWidth
        if (srcX < 0) {
            srcX += texturePixelWidth
        }
        // --- End srcX Calculation ---

        // --- Source Rectangle (Pixels) ---
        // Y and Height come directly from the texture region
        val srcY = region.regionY.toFloat()
        val srcHeight = region.regionHeight.toFloat() // MODIFIED - Use full region height

        // Width needs to cover the horizontal repetitions based on destWidth
        val repetitionsNeeded = MathUtils.ceil(destWidth / transformCmp.width)
        val srcWidth = texturePixelWidth * repetitionsNeeded // Use texturePixelWidth

        // --- End Source Rectangle ---

        engine.batch.draw(
            texture,
            destX, // Destination X (Tiled Rect)
            destY, // Destination Y (Bottom of Tiled Rect) // MODIFIED
            destWidth, // Destination Width (Tiled Rect)
            destHeight, // Destination Height (Original Image Height) // MODIFIED
            srcX.roundToInt(), // Source X (pixels, scrolling offset)
            srcY.roundToInt(), // Source Y (pixels, from region)
            srcWidth.roundToInt(), // Source Width (pixels, enough for repeats) // MODIFIED
            srcHeight.roundToInt(), // Source Height (pixels, full region height) // MODIFIED
            imageCmp.flipImage,
            false,
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
