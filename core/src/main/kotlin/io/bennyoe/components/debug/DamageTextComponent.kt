package io.bennyoe.components.debug

import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.actions.Actions.fadeOut
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import ktx.actors.plusAssign
import ktx.log.logger
import ktx.math.vec2

class DamageTextComponent(
    private val uiStage: Stage,
    var txtLocation: Vector2 = vec2(),
    var lifeSpan: Float = 1f,
) : Component<DamageTextComponent> {
    lateinit var label: Label
    val txtTarget: Vector2 = vec2()
    var time = 0f

    override fun World.onAdd(entity: Entity) {
        label += fadeOut(lifeSpan, Interpolation.pow3OutInverse)
        uiStage.addActor(label)
        txtTarget.set(
            txtLocation.x + MathUtils.random(-1.5f, 1.5f),
            txtLocation.y + 1f,
        )
    }

    override fun World.onRemove(entity: Entity) {
        uiStage.root.removeActor(label)
    }

    override fun type() = DamageTextComponent

    companion object : ComponentType<DamageTextComponent>() {
        val logger = logger<DamageTextComponent>()
    }
}
