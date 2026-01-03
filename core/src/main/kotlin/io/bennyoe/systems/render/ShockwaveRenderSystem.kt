package io.bennyoe.systems.render

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IntervalSystem
import com.github.quillraven.fleks.World.Companion.inject
import io.bennyoe.components.NoiseType
import io.bennyoe.components.TransformComponent
import io.bennyoe.event.NoiseEvent
import io.bennyoe.utility.setupShader
import ktx.graphics.use
import ktx.log.logger
import ktx.math.vec2

const val SHOCKWAVE_DURATION = 1f
const val MAX_SHOCKWAVES = 10

class ShockwaveRenderSystem(
    val stage: Stage = inject("stage"),
) : IntervalSystem(),
    EventListener {
    private val orthoCam = stage.camera as OrthographicCamera
    private val shockwaveShader: ShaderProgram = setupShader("shockwave")
    private val shockwaveQueue = mutableListOf<Shockwave>()
    private val scratchVec = vec2()

    override fun handle(event: Event): Boolean {
        when (event) {
            is NoiseEvent -> {
                if (event.continuous && shockwaveQueue.any { it.entity == event.entity && it.continuous }) {
                    return true
                }

                if (shockwaveQueue.size < MAX_SHOCKWAVES) {
                    shockwaveQueue.add(
                        Shockwave(
                            entity = event.entity,
                            pos = Vector2(event.pos),
                            range = event.range,
                            type = event.type,
                            continuous = event.continuous,
                        ),
                    )
                    return true
                }
            }
        }
        return false
    }

    override fun onTick() {
        shockwaveQueue.removeIf { shockwave ->
            shockwave.time += deltaTime
            shockwave.time >= SHOCKWAVE_DURATION
        }
    }

    fun render(texture: Texture) {
        if (shockwaveQueue.isEmpty()) return

        val w = orthoCam.viewportWidth * orthoCam.zoom
        val h = orthoCam.viewportHeight * orthoCam.zoom
        val x = orthoCam.position.x - w * 0.5f
        val y = orthoCam.position.y - h * 0.5f
        val minDim = minOf(Gdx.graphics.backBufferWidth.toFloat(), Gdx.graphics.backBufferHeight.toFloat())
        val radiusPx = 0.01f * minDim

        val activeCount = minOf(shockwaveQueue.size, MAX_SHOCKWAVES)

        // centers: [x0,y0, x1,y1, ...]
        val centersUv = FloatArray(MAX_SHOCKWAVES * 2)
        val radiiPx = FloatArray(MAX_SHOCKWAVES)
        val times = FloatArray(MAX_SHOCKWAVES)

        for (i in 0 until activeCount) {
            val shockwave = shockwaveQueue[i]
            times[i] = shockwave.time

            val worldPos =
                if (shockwave.continuous) {
                    shockwave.entity.getOrNull(TransformComponent)?.position ?: shockwave.pos
                } else {
                    shockwave.pos
                }

            scratchVec.set(worldPos)
            stage.viewport.project(scratchVec)

            // 1. Project center to screen UV
            scratchVec.set(worldPos)
            stage.viewport.project(scratchVec)
            val centerX = scratchVec.x
            centersUv[i * 2] = centerX / Gdx.graphics.width
            centersUv[i * 2 + 1] = scratchVec.y / Gdx.graphics.height

            // 2. Project a point at 'range' distance to calculate visual radius
            // This automatically handles camera zoom and viewport scaling
            scratchVec.set(worldPos.x + shockwave.range, worldPos.y)
            stage.viewport.project(scratchVec)
            val edgeX = scratchVec.x

            // 3. Convert pixel distance to UV-space distance adjusted by aspect ratio
            // The shader uses: length(dir * vec2(aspect, 1.0))
            val aspect = Gdx.graphics.width.toFloat() / Gdx.graphics.height.toFloat()
            radiiPx[i] = (kotlin.math.abs(edgeX - centerX) / Gdx.graphics.width) * aspect
        }

        stage.batch.use {
            stage.batch.shader = shockwaveShader

            shockwaveShader.setUniformf(
                "u_resolution_px",
                Gdx.graphics.backBufferWidth.toFloat(),
                Gdx.graphics.backBufferHeight.toFloat(),
            )

            shockwaveShader.setUniform1fv("u_times", times, 0, activeCount)

            shockwaveShader.setUniformi("u_shockwave_count", activeCount)

            shockwaveShader.setUniform2fv("u_center_uv", centersUv, 0, activeCount * 2)
            shockwaveShader.setUniform1fv("u_radius_px", radiiPx, 0, activeCount)

            stage.batch.draw(
                texture,
                x,
                y,
                w,
                h,
                0,
                0,
                texture.width,
                texture.height,
                false,
                true,
            )

            stage.batch.shader = null
        }
    }

    companion object {
        val logger = logger<ShockwaveRenderSystem>()
    }
}

data class Shockwave(
    val entity: Entity,
    var pos: Vector2,
    val range: Float,
    val type: NoiseType,
    var time: Float = 0f,
    val continuous: Boolean = false,
) {
    init {
        pos = Vector2(pos)
    }
}
