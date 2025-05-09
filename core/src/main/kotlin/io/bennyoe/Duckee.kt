package io.bennyoe

import io.bennyoe.screens.LoadingScreen
import io.bennyoe.widgets.createSkin
import ktx.app.KtxGame
import ktx.app.KtxScreen
import ktx.inject.Context

class Duckee : KtxGame<KtxScreen>() {
    private val context: Context by lazy { GameContext() }

    override fun create() {
        createSkin()
        addScreen(LoadingScreen(context, this))
        setScreen<LoadingScreen>()
    }

    override fun dispose() {
        super.dispose()
    }
}
