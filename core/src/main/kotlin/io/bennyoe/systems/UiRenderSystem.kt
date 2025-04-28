package io.bennyoe.systems

import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import com.github.quillraven.fleks.World.Companion.inject
import io.bennyoe.components.UiComponent
import ktx.log.logger

class UiRenderSystem(
    private val uiStage: Stage = inject("uiStage"),
) : IteratingSystem(family { all(UiComponent) }, enabled = true) {
    override fun onTick() {
        with(uiStage) {
            uiStage.viewport.apply()
            uiStage.act()
            uiStage.draw()

            super.onTick()
        }
    }

    override fun onTickEntity(entity: Entity) {
    }

    companion object {
        val logger = logger<UiRenderSystem>()
    }
}
