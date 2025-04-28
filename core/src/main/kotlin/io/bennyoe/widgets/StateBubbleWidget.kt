package io.bennyoe.widgets

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup
import ktx.actors.plusAssign
import ktx.scene2d.label
import ktx.scene2d.scene2d

class StateBubbleWidget : WidgetGroup() {
    var state = ""
    private var displayScore =
        scene2d.label(state) {
            setFontScale(0.1f)
        }

    init {
        debug = true
        setSize(1280f, 1024f)
        setPosition(0f, 0f)

        this += displayScore
    }

    fun displayState(state: String) {
        displayScore.setText(state)
    }

    fun setPosition(position: Vector2) {
        displayScore.setPosition(position.x, position.y)
    }
}
