package io.bennyoe.systems

import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer
import com.badlogic.gdx.physics.box2d.World
import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.quillraven.fleks.IntervalSystem
import com.github.quillraven.fleks.World.Companion.inject
import ktx.assets.disposeSafely
import ktx.log.logger

class DebugSystem(
    private val phyWorld: World = inject("phyWorld"),
    private val stage: Stage = inject()
) : IntervalSystem(enabled = false) {
    private lateinit var physicsRenderer: Box2DDebugRenderer

    init {
        if (enabled) {
            LOG.debug { "Debug started" }
            physicsRenderer = Box2DDebugRenderer()
        }
    }

    override fun onTick() {
        physicsRenderer.render(phyWorld, stage.camera.combined)
    }

    override fun onDispose() {
        if (enabled) {
            physicsRenderer.disposeSafely()
        }
    }

    companion object {
        private val LOG = logger<DebugSystem>()
    }
}
