package io.bennyoe.ui

import com.badlogic.gdx.graphics.profiling.GLProfiler
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.Window
import io.bennyoe.ui.widgets.CharacterInfoWidget
import io.bennyoe.ui.widgets.DebugWidget
import ktx.scene2d.KTable

class GameView(
    skin: Skin,
    profiler: GLProfiler,
) : Table(skin),
    KTable {
    private val characterInfoWidget: CharacterInfoWidget = CharacterInfoWidget(skin)
    val debugWindow =
        Window("Debug Window", skin).apply {
            isMovable = true
            isResizable = true
            touchable = Touchable.enabled
            width = 400f
            height = 500f
            padTop(29f)
            setResizeBorder(12)
            keepWithinStage()

            add(DebugWidget(skin, profiler)).fill().center().expand()
            isVisible = false
        }

    init {
        setFillParent(true)
        defaults().pad(8f)

        add().expand().fill()

        row()

        add(characterInfoWidget)
            .colspan(2)
            .bottom()
            .center()
            .expandX()
            .padBottom(12f)
    }

    fun toggleDebugOverlay() {
        debugWindow.isVisible = !debugWindow.isVisible
    }

    fun playerLife(value: Float) {
        characterInfoWidget.lifeBar.value = value
    }

    fun playerAir(value: Float) {
        characterInfoWidget.airBar.value = value
    }
}
