package io.bennyoe.ui.widgets

import com.badlogic.gdx.graphics.profiling.GLProfiler
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import io.bennyoe.ui.Drawables
import io.bennyoe.ui.widgets.debug.DrawCallsCounterWidget
import io.bennyoe.ui.widgets.debug.FpsCounterWidget
import io.bennyoe.ui.widgets.debug.FpsMillis
import ktx.actors.alpha
import ktx.scene2d.KTable
import ktx.style.get

class DebugView(
    skin: Skin,
    profiler: GLProfiler,
) : Table(skin),
    KTable {
    private val fpsCounter = FpsCounterWidget(skin["debug"])
    private val fpsMillis = FpsMillis(skin["debug"])
    private val drawCallsCounter = DrawCallsCounterWidget(skin["debug"], profiler)

    init {
        background = skin.getDrawable(Drawables.DEBUG_FRAME.atlasKey)
        alpha = 0.8f
        pad(15f)

        add(fpsCounter).left().row()
        add(fpsMillis).left().row()
        add(drawCallsCounter).left().row()

        left().top()
    }
}
