package io.bennyoe.components

import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import io.bennyoe.widgets.StateBubbleWidget

class StateBubbleComponent(
    private val stage: Stage,
) : Component<StateBubbleComponent> {
    val bubble by lazy { StateBubbleWidget() }

    override fun World.onAdd(entity: Entity) {
        stage.addActor(bubble)
    }

    override fun type() = StateBubbleComponent

    companion object : ComponentType<StateBubbleComponent>()
}
