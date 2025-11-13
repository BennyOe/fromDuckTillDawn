package io.bennyoe.screens

import io.bennyoe.Stages
import ktx.app.KtxScreen
import ktx.inject.Context

abstract class AbstractScreen(
    val context: Context,
) : KtxScreen {
    override fun resize(
        width: Int,
        height: Int,
    ) {
        super.resize(width, height)
        context
            .inject<Stages>()
            .stage.viewport
            .update(width, height, false)
        context
            .inject<Stages>()
            .uiStage.viewport
            .update(width, height, true)
    }
}
