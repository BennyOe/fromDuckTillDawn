package io.bennyoe.components

import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import io.bennyoe.widgets.StateBubbleWidget
import ktx.log.logger

class StateBubbleComponent(
    private val uiStage: Stage,
) : Component<StateBubbleComponent> {
    val bubble by lazy { StateBubbleWidget() }

    override fun World.onAdd(entity: Entity) {
        uiStage.addActor(bubble)
    }

    override fun World.onRemove(entity: Entity) {
        bubble.remove()
    }

    override fun type() = StateBubbleComponent

    companion object : ComponentType<StateBubbleComponent>() {
        val logger = logger<StateBubbleComponent>()
    }
}
