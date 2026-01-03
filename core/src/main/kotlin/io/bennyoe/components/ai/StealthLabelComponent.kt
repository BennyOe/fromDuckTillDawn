package io.bennyoe.components.ai

import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import ktx.math.vec2
import ktx.scene2d.Scene2DSkin

class StealthLabelComponent(
    private val uiStage: Stage,
    var txtLocation: Vector2 = vec2(),
) : Component<StealthLabelComponent> {
    var label: Label = Label("", Scene2DSkin.defaultSkin)
    val txtTarget: Vector2 = vec2()

    override fun World.onAdd(entity: Entity) {
        uiStage.addActor(label)
        txtTarget.set(
            txtLocation.x + MathUtils.random(-1.5f, 1.5f),
            txtLocation.y - 1f,
        )
    }

    override fun World.onRemove(entity: Entity) {
        uiStage.root.removeActor(label)
    }

    override fun type() = StealthLabelComponent

    companion object : ComponentType<StealthLabelComponent>() {
    }
}
