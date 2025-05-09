package io.bennyoe

import com.badlogic.gdx.Application.LOG_DEBUG
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.maps.tiled.TiledMap
import com.badlogic.gdx.maps.tiled.TmxMapLoader
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.utils.viewport.ExtendViewport
import io.bennyoe.GameConstants.GAME_HEIGHT
import io.bennyoe.GameConstants.GAME_WIDTH
import io.bennyoe.GameConstants.WORLD_HEIGHT
import io.bennyoe.GameConstants.WORLD_WIDTH
import ktx.assets.async.AssetStorage
import ktx.async.KtxAsync
import ktx.inject.Context
import ktx.inject.register

class GameContext : Context() {
    init {
        Gdx.app.logLevel = LOG_DEBUG
        val spriteBatch: SpriteBatch by lazy { SpriteBatch() }
        val stages = Stages(spriteBatch)
        val shapeRenderer = ShapeRenderer()
        val assets: AssetStorage by lazy {
            KtxAsync.initiate() // has to be called before using coroutines
            AssetStorage() // the KTX extension for the asset manager
        }

        // set TmxMapLoader as custom loader for the tmx map files
        assets.setLoader(TiledMap::class.java, "tmx") {
            TmxMapLoader(InternalFileHandleResolver())
        }

        register { bindSingleton(assets) }
        register { bindSingleton(stages) }
        register { bindSingleton(shapeRenderer) }
        register { bindSingleton(spriteBatch) }
    }

    override fun dispose() {
        super.dispose()
    }
}

class Stages(
    val spriteBatch: SpriteBatch,
) {
    val stage by lazy { Stage(ExtendViewport(WORLD_WIDTH, WORLD_HEIGHT), spriteBatch) }
    val uiStage by lazy { Stage(ExtendViewport(GAME_WIDTH, GAME_HEIGHT), spriteBatch) }
}
