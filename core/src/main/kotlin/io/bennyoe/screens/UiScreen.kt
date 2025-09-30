package io.bennyoe.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import io.bennyoe.Stages
import io.bennyoe.ui.GameView
import io.bennyoe.ui.gameView
import ktx.inject.Context
import ktx.scene2d.actors

class UiScreen(
    context: Context,
) : AbstractScreen(context) {
    private val stages = context.inject<Stages>()
    private val uiStage = stages.uiStage
    private lateinit var gameView: GameView

    override fun resize(
        width: Int,
        height: Int,
    ) {
        uiStage.viewport.update(width, height, true)
    }

    override fun show() {
        uiStage.clear()
//        uiStage.isDebugAll = true
        uiStage.actors {
            gameView = gameView()
        }
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

        uiStage.act()
        uiStage.draw()
    }

    override fun dispose() {
        uiStage.dispose()
    }
}
