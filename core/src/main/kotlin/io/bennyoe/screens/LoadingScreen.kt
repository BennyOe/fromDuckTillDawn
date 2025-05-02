package io.bennyoe.screens

import io.bennyoe.Duckee
import io.bennyoe.assets.MapAssets
import io.bennyoe.assets.TextureAssets
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import ktx.app.KtxScreen
import ktx.assets.async.AssetStorage
import ktx.async.KtxAsync
import ktx.collections.gdxArrayOf
import ktx.inject.Context
import ktx.log.logger

class LoadingScreen(
    val context: Context,
) : KtxScreen {
    val assets = context.inject<AssetStorage>()
    val game = context.inject<Duckee>()

    override fun show() {
        val timeStampBeforeLoading = System.currentTimeMillis()
        val assetRefs =
            gdxArrayOf(
                TextureAssets.entries.map { assets.loadAsync(it.descriptor) },
                MapAssets.entries.map { assets.loadAsync(it.descriptor) },
            ).flatten()

        KtxAsync.launch {
            assetRefs.joinAll()
            logger.debug { "Assets loaded in: ${System.currentTimeMillis() - timeStampBeforeLoading}ms" }
            assetsLoaded()
        }
    }

    private fun assetsLoaded() {
        game.addScreen(GameScreen(context))
        game.setScreen<GameScreen>()
        this.dispose()
    }

    companion object {
        val logger = logger<LoadingScreen>()
    }
}
