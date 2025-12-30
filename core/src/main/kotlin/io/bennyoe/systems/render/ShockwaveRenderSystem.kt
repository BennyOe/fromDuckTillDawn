package io.bennyoe.systems.render

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.quillraven.fleks.IntervalSystem
import com.github.quillraven.fleks.World.Companion.inject
import io.bennyoe.components.NoiseType
import io.bennyoe.event.NoiseEvent
import io.bennyoe.utility.setupShader
import ktx.graphics.use
import ktx.log.logger

const val SHOCKWAVE_DURATION = 4f
const val MAX_SHOCKWAVES = 10

class ShockwaveRenderSystem(
    val stage: Stage = inject("stage"),
) : IntervalSystem(),
    EventListener {
    private val orthoCam = stage.camera as OrthographicCamera
    private val shockwaveShader: ShaderProgram = setupShader("shockwave")
    private val shockwaveQueue = mutableListOf<Shockwave>()

    override fun handle(event: Event?): Boolean {
        when (event) {
            is NoiseEvent -> {
                if (shockwaveQueue.size < MAX_SHOCKWAVES) {
                    shockwaveQueue.add(
                        Shockwave(
                            pos = Vector2(event.pos),
                            range = event.range,
                            loudness = event.loudness,
                            type = event.type,
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

            val screenPos = Vector2(shockwave.pos)
            stage.viewport.project(screenPos)

            centersUv[i * 2] = screenPos.x / Gdx.graphics.width
            centersUv[i * 2 + 1] = screenPos.y / Gdx.graphics.height
            radiiPx[i] = radiusPx
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
    var pos: Vector2,
    val range: Float,
    val loudness: Float,
    val type: NoiseType,
    var time: Float = 0f,
) {
    init {
        pos = Vector2(pos)
    }
}
