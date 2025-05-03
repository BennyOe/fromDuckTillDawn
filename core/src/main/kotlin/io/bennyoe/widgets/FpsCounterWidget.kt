package io.bennyoe.widgets

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle
import ktx.log.logger

class FpsCounterWidget(
    style: LabelStyle,
) : Label("", style) {
    private var timer = 0f

    init {
        setPosition(10f, 20f)
    }

    override fun act(delta: Float) {
        super.act(delta)
        timer += delta
        if (timer >= 0.5f) {
            setText("${Gdx.graphics.framesPerSecond} fps")
            timer = 0f
        }
    }

    companion object {
        val logger = logger<FpsCounterWidget>()
    }
}
