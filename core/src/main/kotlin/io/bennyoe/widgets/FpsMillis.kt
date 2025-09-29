package io.bennyoe.widgets

import com.badlogic.gdx.scenes.scene2d.ui.Label
import ktx.log.logger

class FpsMillis(
    style: LabelStyle,
) : Label("", style) {
    private var acc = 0f
    private var frames = 0
    private var maxDt = 0f

    init {
        setPosition(10f, 20f)
    }

    override fun act(delta: Float) {
        super.act(delta)
        maxDt = maxOf(maxDt, delta)
        acc += delta
        frames++
        if (acc >= 1f) {
            setText("Millis avg=%.2fms  max=%.2fms".format((acc / frames) * 1000f, maxDt * 1000f))
            acc = 0f
            frames = 0
            maxDt = 0f
        }
    }

    companion object {
        val logger = logger<FpsMillis>()
    }
}
