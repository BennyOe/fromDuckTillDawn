package io.bennyoe

import com.badlogic.gdx.utils.viewport.FitViewport
import io.bennyoe.screens.GameScreen
import ktx.app.KtxGame
import ktx.app.KtxScreen
import ktx.inject.Context

class Duckee : KtxGame<KtxScreen>() {
    private val context: Context by lazy { GameContext() }

    override fun create() {
        addScreen(GameScreen(context))
        setScreen<GameScreen>()
    }

    override fun dispose() {
        context.dispose()
        super.dispose()
    }

    override fun resize(width: Int, height: Int) {
        super.resize(width, height)
        context.inject<FitViewport>().update(width, height, true)
    }
}
