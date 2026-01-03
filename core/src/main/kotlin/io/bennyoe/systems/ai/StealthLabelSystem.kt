package io.bennyoe.systems.ai

import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import com.github.quillraven.fleks.World.Companion.inject
import io.bennyoe.components.PhysicComponent
import io.bennyoe.components.TransformComponent
import io.bennyoe.components.ai.StealthLabelComponent
import io.bennyoe.systems.DamageSystem
import ktx.log.logger
import ktx.math.vec2

class StealthLabelSystem(
    private val uiStage: Stage = inject("uiStage"),
    private val stage: Stage = inject("stage"),
) : IteratingSystem(family { all(StealthLabelComponent, PhysicComponent) }) {
    private var uiLocation = vec2()

    override fun onTickEntity(entity: Entity) {
        val stealthLabelCmp = entity[StealthLabelComponent]
        val physicCmp = entity[PhysicComponent]
        stealthLabelCmp.txtLocation.set(
            physicCmp.body.position.x - 4f,
            physicCmp.body.position.y + physicCmp.size.y * 0.5f,
        )

        with(stealthLabelCmp) {
            uiLocation = txtLocation.cpy()
            stage.viewport.project(uiLocation)
            uiStage.viewport.unproject(vec2(uiLocation.x, -uiLocation.y))
            label.setPosition(uiLocation.x, uiLocation.y)
        }
    }
}
