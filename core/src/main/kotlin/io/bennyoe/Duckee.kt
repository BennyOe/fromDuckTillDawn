package io.bennyoe

import com.badlogic.gdx.Application.LOG_DEBUG
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import io.bennyoe.screens.GameScreen
import ktx.app.KtxGame
import ktx.app.KtxScreen
import ktx.async.KtxAsync
import ktx.inject.Context
import ktx.inject.register

class Duckee : KtxGame<KtxScreen>() {
    private val spriteBatch: SpriteBatch by lazy { SpriteBatch() }
    private val context: Context by lazy {
        Context().apply {
            register { bindSingleton(spriteBatch) }
        }
    }

    override fun create() {
        KtxAsync.initiate()

        addScreen(GameScreen(context))
        setScreen<GameScreen>()
        Gdx.app.logLevel = LOG_DEBUG
    }

    override fun dispose() {
        context.dispose()
        spriteBatch.dispose()
        super.dispose()
    }
}

