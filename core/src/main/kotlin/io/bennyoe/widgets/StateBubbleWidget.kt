package io.bennyoe.widgets

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup
import com.badlogic.gdx.utils.Align
import ktx.actors.plusAssign
import ktx.scene2d.scene2d
import ktx.scene2d.textField

class StateBubbleWidget : WidgetGroup() {
    var state = ""

    private var displayState =
        scene2d.textField(state) {
            setPosition(100f, 100f)
            setSize(130f, 30f)
            alignment = Align.center
        }

    init {
        debug = true

        this += displayState
    }

    fun displayState(state: String) {
        displayState.setText(state)
    }

    fun setPosition(position: Vector2) {
        displayState.setPosition(position.x, position.y)
    }
}
