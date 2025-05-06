package io.bennyoe.systems

import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.quillraven.fleks.IntervalSystem
import com.github.quillraven.fleks.World.Companion.inject
import ktx.log.logger

class UiRenderSystem(
    private val uiStage: Stage = inject("uiStage"),
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
