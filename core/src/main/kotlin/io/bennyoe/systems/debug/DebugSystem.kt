package io.bennyoe.systems.debug

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer
import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.quillraven.fleks.IntervalSystem
import com.github.quillraven.fleks.World
import io.bennyoe.components.PlayerComponent
import io.bennyoe.components.UiComponent
import io.bennyoe.components.debug.DebugComponent
import io.bennyoe.components.debug.StateBubbleComponent
import ktx.assets.disposeSafely
import ktx.log.logger
import com.badlogic.gdx.physics.box2d.World as PhyWorld

class DebugSystem(
    private val phyWorld: PhyWorld =
        World.Companion
            .inject("phyWorld"),
    private val stage: Stage =
        World.Companion
            .inject("stage"),
    private val uiStage: Stage =
        World.Companion
            .inject("uiStage"),
) : IntervalSystem(enabled = true) {
    val physicsRenderer by lazy { Box2DDebugRenderer() }

    override fun onTick() {
        val debugEntity = world.family { all(DebugComponent.Companion) }.firstOrNull()
        require(debugEntity != null)
        val debugCmp = debugEntity.let { entity -> entity[DebugComponent.Companion] }

        val playerEntity = world.family { all(PlayerComponent.Companion) }.firstOrNull() ?: return

        if (debugCmp.enabled) {
            if (playerEntity hasNo StateBubbleComponent.Companion) {
                world.entity { playerEntity += StateBubbleComponent(uiStage) }
            }
            if (playerEntity hasNo UiComponent) {
                world.entity { playerEntity += UiComponent }
            }
            if (!uiStage.actors.contains(debugCmp.fpsCounterWidget)) {
                uiStage.addActor(debugCmp.fpsCounterWidget)
            }

            physicsRenderer.render(phyWorld, stage.camera.combined)
            debugCmp.fpsCounterWidget.displayFps(Gdx.graphics.framesPerSecond.toString())
        } else {
            if (playerEntity has StateBubbleComponent.Companion) {
                playerEntity.configure { it -= StateBubbleComponent.Companion }
            }
            if (playerEntity has UiComponent) {
                playerEntity.configure { it -= UiComponent }
            }
            debugCmp.fpsCounterWidget.remove()
        }
    }

    override fun onDispose() {
        if (enabled) {
            physicsRenderer.disposeSafely()
        }
    }

    companion object {
        private val logger = logger<DebugSystem>()
    }
}
