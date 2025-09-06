package io.bennyoe.systems.render

import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.quillraven.fleks.IntervalSystem
import com.github.quillraven.fleks.World
import ktx.log.logger

class UiRenderSystem(
    private val uiStage: Stage = World.Companion.inject("uiStage"),
) : IntervalSystem(enabled = true) {
    override fun onTick() {
        with(uiStage) {
            viewport.apply()
            camera.update()
            act(deltaTime)
            draw()
        }
    }

    companion object {
        val logger = logger<UiRenderSystem>()
    }
}
