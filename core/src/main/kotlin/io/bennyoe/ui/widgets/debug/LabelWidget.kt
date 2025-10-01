package io.bennyoe.ui.widgets.debug

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup
import ktx.actors.plusAssign
import ktx.log.logger
import ktx.scene2d.label
import ktx.scene2d.scene2d

class LabelWidget(
    val label: String,
    val myColor: Color = Color.RED,
) : WidgetGroup() {
    var displayLabel =
        scene2d.label(label) {
            color = myColor
        }

    init {
        this += displayLabel
    }

    companion object {
        val logger = logger<LabelWidget>()
    }
}
