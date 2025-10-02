package io.bennyoe.ui.widgets.debug

import com.badlogic.gdx.graphics.profiling.GLProfiler
import com.badlogic.gdx.scenes.scene2d.ui.Label
import ktx.log.logger

class DrawCallsCounterWidget(
    style: LabelStyle,
    val profiler: GLProfiler,
) : Label("", style) {
    private var timer = 0f

    override fun act(delta: Float) {
        super.act(delta)
        timer += delta
        if (timer >= 0.5f) {
            setText("DrawCalls: ${profiler.drawCalls}")
            timer = 0f
        }
    }

    companion object {
        val logger = logger<DrawCallsCounterWidget>()
    }
}
