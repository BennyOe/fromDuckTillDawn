package io.bennyoe

import com.badlogic.gdx.scenes.scene2d.Stage
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
        context.inject<Stage>().viewport.update(width, height, true)
    }
    companion object {
        const val UNIT_SCALE = 1 / 16f
    }
}
