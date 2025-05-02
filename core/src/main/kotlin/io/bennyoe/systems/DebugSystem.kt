package io.bennyoe.systems

import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer
import com.badlogic.gdx.physics.box2d.World
import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.quillraven.fleks.IntervalSystem
import com.github.quillraven.fleks.World.Companion.inject
import io.bennyoe.components.DebugComponent
import io.bennyoe.components.PlayerComponent
import io.bennyoe.components.StateBubbleComponent
import io.bennyoe.components.UiComponent
import ktx.assets.disposeSafely
import ktx.log.logger

class DebugSystem(
    private val phyWorld: World = inject("phyWorld"),
    private val stage: Stage = inject("stage"),
    private val uiState: Stage = inject("uiStage"),
) : IntervalSystem(enabled = true) {
    val physicsRenderer by lazy { Box2DDebugRenderer() }

    override fun onTick() {
        val debugEntity = world.family { all(DebugComponent) }.firstOrNull()
        val debugCmp = debugEntity?.let { entity -> entity[DebugComponent] }
        val playerEntity = world.family { all(PlayerComponent) }.firstOrNull() ?: return

        if (debugCmp?.enabled == true) {
            if (playerEntity hasNo StateBubbleComponent) {
                world.entity { playerEntity += StateBubbleComponent(uiState) }
            }
            if (playerEntity hasNo UiComponent) {
                world.entity { playerEntity += UiComponent }
            }
            stage.isDebugAll = true
            physicsRenderer.render(phyWorld, stage.camera.combined)
        } else {
            if (playerEntity has StateBubbleComponent) {
                playerEntity.configure { it -= StateBubbleComponent }
            }
            if (playerEntity has UiComponent) {
                playerEntity.configure { it -= UiComponent }
            }
            stage.isDebugAll = false
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
