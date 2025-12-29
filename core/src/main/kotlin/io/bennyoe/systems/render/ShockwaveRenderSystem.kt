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

const val SHOCKWAVE_DURATION = 1f

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
                shockwaveQueue.add(
                    Shockwave(
                        event.pos,
                        event.range,
                        event.loudness,
                        event.type,
                    ),
                )
                return true
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

        stage.batch.use {
            stage.batch.shader = shockwaveShader

            shockwaveQueue.forEach { shockwave ->
                // world -> screen (pixel)
                val screenPos = Vector2(shockwave.pos)
                stage.viewport.project(screenPos)

                shockwaveShader.setUniformf("u_center_px", screenPos.x, screenPos.y)
                shockwaveShader.setUniformf("u_radius_px", radiusPx)

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
            }

            stage.batch.shader = null
        }
    }

    companion object {
        val logger = logger<ShockwaveRenderSystem>()
    }
}

data class Shockwave(
    val pos: Vector2,
    val range: Float,
    val loudness: Float,
    val type: NoiseType,
    var time: Float = 0f,
)
