package io.bennyoe

import io.bennyoe.screens.LoadingScreen
import io.bennyoe.widgets.createSkin
import ktx.app.KtxGame
import ktx.app.KtxScreen
import ktx.inject.Context

class Duckee : KtxGame<KtxScreen>() {
    private val context: Context by lazy { GameContext(this) }

    override fun create() {
        createSkin()
        addScreen(LoadingScreen(context))
        setScreen<LoadingScreen>()
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
        context
            .inject<Stages>()
            .stage.viewport
            .update(width, height, true)
    }
}
