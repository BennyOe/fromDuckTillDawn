package io.bennyoe.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.profiling.GLProfiler
import io.bennyoe.Stages
import io.bennyoe.ui.GameView
import ktx.inject.Context
import ktx.scene2d.Scene2DSkin

class UiScreen(
    context: Context,
) : AbstractScreen(context) {
    private val profiler by lazy { GLProfiler(Gdx.graphics) }
    private val stages = context.inject<Stages>()
    private val uiStage = stages.uiStage
    private val gameView: GameView = GameView(Scene2DSkin.defaultSkin, profiler)

    override fun resize(
        width: Int,
        height: Int,
    ) {
        uiStage.viewport.update(width, height, true)
    }

    override fun show() {
        uiStage.clear()
        uiStage.isDebugAll = true
        uiStage.addActor(gameView)
    }

    override fun render(delta: Float) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            hide()
            show()
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.Q)) {
            gameView.playerLife(0f)
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.W)) {
            gameView.playerLife(0.5f)
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.E)) {
            gameView.playerLife(1f)
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.BACKSPACE)) {
            gameView.toggleDebugOverlay()
        }

        uiStage.act()
        uiStage.draw()
    }

    override fun dispose() {
        uiStage.dispose()
    }
}
