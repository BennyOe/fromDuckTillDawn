package io.bennyoe

import com.badlogic.gdx.Application.LOG_DEBUG
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.maps.tiled.TiledMap
import com.badlogic.gdx.maps.tiled.TmxMapLoader
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.utils.viewport.FitViewport
import com.badlogic.gdx.utils.viewport.ScreenViewport
import de.pottgames.tuningfork.Audio
import de.pottgames.tuningfork.AudioConfig
import de.pottgames.tuningfork.DistanceAttenuationModel
import de.pottgames.tuningfork.SoundBuffer
import de.pottgames.tuningfork.SoundBufferLoader
import io.bennyoe.config.GameConstants.ENABLE_DEBUG
import io.bennyoe.config.GameConstants.WORLD_HEIGHT
import io.bennyoe.config.GameConstants.WORLD_WIDTH
import io.bennyoe.service.DefaultDebugRenderService
import io.bennyoe.service.NoOpDebugRenderService
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
        val debugRenderService = if (ENABLE_DEBUG) DefaultDebugRenderService() else NoOpDebugRenderService()
        val config =
            AudioConfig().apply {
                distanceAttenuationModel = DistanceAttenuationModel.LINEAR_DISTANCE_CLAMPED
            }
        val audio: Audio = Audio.init(config)
        val assets: AssetStorage by lazy {
            KtxAsync.initiate() // has to be called before using coroutines
            AssetStorage() // the KTX extension for the asset manager
        }

        // set TmxMapLoader as custom loader for the tmx map files
        assets.setLoader(TiledMap::class.java, "tmx") {
            TmxMapLoader(InternalFileHandleResolver())
        }

        // set SoundBuffer as custom loader for mp3 files
        assets.setLoader(SoundBuffer::class.java, "mp3") {
            SoundBufferLoader(InternalFileHandleResolver())
        }

        register { bindSingleton(assets) }
        register { bindSingleton(audio) }
        register { bindSingleton(stages) }
        register { bindSingleton(shapeRenderer) }
        register { bindSingleton(spriteBatch) }
        register { bindSingleton(debugRenderService) }
    }

    override fun dispose() {
        super.dispose()
    }
}

data class Stages(
    val spriteBatch: SpriteBatch,
) {
    val stage by lazy { Stage(FitViewport(WORLD_WIDTH, WORLD_HEIGHT), spriteBatch) }
    val uiStage by lazy { Stage(ScreenViewport(), spriteBatch) }
}
