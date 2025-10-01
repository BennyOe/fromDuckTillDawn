package io.bennyoe.ui.widgets.debug

import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup
import com.badlogic.gdx.utils.Align
import ktx.actors.plusAssign
import ktx.log.logger
import ktx.scene2d.scene2d
import ktx.scene2d.textField

class StateBubbleWidget : WidgetGroup() {
    var displayState =
        scene2d.textField {
            alignment = Align.center
        }

    init {
        this += displayState
    }

    fun displayState(state: String) {
        displayState.setText(state)
        displayState.setSize(calculateWidth(state), 30f)
    }

    private fun calculateWidth(text: String): Float {
        val font = displayState.style.font
        val layout =
            GlyphLayout(font, text)
        return layout.width + 20f
    }

    companion object {
        val logger = logger<StateBubbleWidget>()
    }
}
