package io.bennyoe.systems.debug

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle
import com.github.quillraven.fleks.IntervalSystem
import com.github.quillraven.fleks.World
import io.bennyoe.components.PlayerComponent
import io.bennyoe.components.UiComponent
import io.bennyoe.components.debug.DebugComponent
import io.bennyoe.components.debug.StateBubbleComponent
import io.bennyoe.widgets.FpsCounterWidget
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
    private val physicsRenderer by lazy { Box2DDebugRenderer() }
    private val fpsLabelStyle = LabelStyle(BitmapFont().apply { data.setScale(1.5f) }, Color(0f, 1f, 0f, 1f))
    private val fpsCounter =
        FpsCounterWidget(fpsLabelStyle).apply {
            setPosition(10f, 20f)
        }

    init {
        uiStage.addActor(fpsCounter)
    }

    override fun onTick() {
        val debugEntity = world.family { all(DebugComponent.Companion) }.firstOrNull() ?: return
        val debugCmp = debugEntity.let { entity -> entity[DebugComponent.Companion] }

        val playerEntity = world.family { all(PlayerComponent.Companion) }.firstOrNull() ?: return

        fpsCounter.isVisible = debugCmp.enabled
        if (debugCmp.enabled) {
            if (playerEntity hasNo StateBubbleComponent) {
                world.entity { playerEntity += StateBubbleComponent(uiStage) }
            }
            if (playerEntity hasNo UiComponent) {
                world.entity { playerEntity += UiComponent }
            }
            fpsCounter.act(deltaTime)
            physicsRenderer.render(phyWorld, stage.camera.combined)
        } else {
            if (playerEntity has StateBubbleComponent) {
                playerEntity.configure { it -= StateBubbleComponent }
            }
            if (playerEntity has UiComponent) {
                playerEntity.configure { it -= UiComponent }
            }
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
