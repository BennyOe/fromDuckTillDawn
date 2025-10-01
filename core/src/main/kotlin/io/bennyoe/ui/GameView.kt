package io.bennyoe.ui

import com.badlogic.gdx.graphics.profiling.GLProfiler
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Container
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.Value
import com.badlogic.gdx.utils.Align
import io.bennyoe.ui.widgets.CharacterInfo
import io.bennyoe.ui.widgets.DebugView
import ktx.scene2d.KTable

class GameView(
    skin: Skin,
    profiler: GLProfiler,
) : Table(skin),
    KTable {
    private val debugView: DebugView =
        DebugView(skin, profiler).apply {
            touchable = Touchable.disabled
        }

    private val debugContainer: Container<DebugView> =
        Container(debugView).apply {
            align(Align.topLeft)
            fill()
            isVisible = false
        }

    private val characterInfo: CharacterInfo = CharacterInfo(skin)

    init {
        setFillParent(true)
        defaults().pad(8f)

        // --- Row 1: Main content area ---
        // Left cell: the debug overlay container (fixed width), expands vertically and keeps its child centered.
        add(debugContainer)
            .width(Value.percentWidth(0.25f, this))
            .height(Value.percentHeight(0.25f, this))
            .top()
            .left()

        add().expand().fill()

        row()

        // --- Row 2: Bottom status bar ---
        // The character info spans the entire width and sits bottom + centered.
        add(characterInfo)
            .colspan(2)
            .bottom()
            .center()
            .expandX()
            .padBottom(12f)
    }

    fun toggleDebugOverlay() {
        debugContainer.isVisible = !debugContainer.isVisible
    }

    fun playerLife(value: Float) {
        characterInfo.lifeBar.value = value
    }
}
