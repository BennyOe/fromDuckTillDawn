package io.bennyoe.systems.debug

import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import com.github.quillraven.fleks.World.Companion.inject
import io.bennyoe.components.debug.DamageTextComponent
import io.bennyoe.systems.DamageSystem
import ktx.log.logger
import ktx.math.vec2

class DamageTextSystem(
    private val uiStage: Stage = inject("uiStage"),
    private val stage: Stage = inject("stage"),
) : IteratingSystem(family { all(DamageTextComponent) }) {
    private var uiLocation = vec2()
    private var uiTarget = vec2()

    override fun onTickEntity(entity: Entity) {
        val damageTextCmp = entity[DamageTextComponent]

        with(damageTextCmp) {
            if (time >= lifeSpan) {
                entity.configure { it -= DamageTextComponent }
                return
            }
            uiLocation = txtLocation.cpy()
            stage.viewport.project(uiLocation)
            uiStage.viewport.unproject(uiLocation)

            uiTarget = txtTarget.cpy()
            stage.viewport.project(uiTarget)
            uiStage.viewport.unproject(uiTarget)

            uiLocation.interpolate(uiTarget, (time / lifeSpan).coerceAtMost(1f), Interpolation.smooth2)
            label.setPosition(uiLocation.x, uiLocation.y)

            time += deltaTime
        }
    }

    companion object {
        val logger = logger<DamageSystem>()
    }
}
