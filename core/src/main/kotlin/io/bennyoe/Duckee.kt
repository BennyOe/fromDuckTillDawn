package io.bennyoe

import io.bennyoe.screens.GameScreen
import io.bennyoe.widgets.createSkin
import ktx.app.KtxGame
import ktx.app.KtxScreen
import ktx.inject.Context

class Duckee : KtxGame<KtxScreen>() {
    private val context: Context by lazy { GameContext() }

    override fun create() {
        createSkin()
        addScreen(GameScreen(context))
        setScreen<GameScreen>()
    }

    override fun dispose() {
        context.dispose()
        super.dispose()
    }

    override fun resize(
        width: Int,
        height: Int,
    ) {
        super.resize(width, height)
        val ctx = context.inject<Stages>()
        ctx.stage.viewport.update(width, height, true)
    }

    companion object {
        const val UNIT_SCALE = 1 / 16f
        const val PHYSIC_TIME_STEP = 1 / 45f
        const val GAME_WIDTH = 1280f
        const val GAME_HEIGHT = 1024f
        const val WORLD_WIDTH = 16f
        const val WORLD_HEIGHT = 9f
    }
}
