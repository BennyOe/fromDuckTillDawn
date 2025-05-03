package io.bennyoe.widgets

import com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup
import ktx.actors.plusAssign
import ktx.log.logger
import ktx.scene2d.label
import ktx.scene2d.scene2d

class FpsCounterWidget : WidgetGroup() {
    var fps = ""

    private var displayFps =
        scene2d.label(fps) {
            setPosition(10f, 20f)
        }

    init {
        this += displayFps
    }

    fun displayFps(fps: String) {
        displayFps.setText("$fps Fps")
    }

    companion object {
        val logger = logger<FpsCounterWidget>()
    }
}
