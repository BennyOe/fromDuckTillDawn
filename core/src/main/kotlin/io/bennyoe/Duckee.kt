package io.bennyoe

import io.bennyoe.screens.LoadingScreen
import io.bennyoe.screens.UiScreen
import io.bennyoe.widgets.createSkin
import io.bennyoe.widgets.disposeSkin
import ktx.app.KtxGame
import ktx.app.KtxScreen
import ktx.inject.Context

class Duckee : KtxGame<KtxScreen>() {
    private val context: Context by lazy { GameContext() }

    override fun create() {
        createSkin()
        addScreen(LoadingScreen(context, this))
        addScreen(UiScreen(context))
//        setScreen<LoadingScreen>()
        setScreen<UiScreen>()
    }

    override fun dispose() {
        disposeSkin()
        super.dispose()
    }
}
