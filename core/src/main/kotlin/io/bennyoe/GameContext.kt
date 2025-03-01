package io.bennyoe

import com.badlogic.gdx.Application.LOG_DEBUG
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.utils.viewport.ExtendViewport
import ktx.inject.Context
import ktx.inject.register

class GameContext : Context(){
    init {
        Gdx.app.logLevel = LOG_DEBUG
        val spriteBatch: SpriteBatch by lazy { SpriteBatch() }
        val stage by lazy { Stage(ExtendViewport(16f, 9f), spriteBatch) }

        register { bindSingleton(stage) }
    }
}
