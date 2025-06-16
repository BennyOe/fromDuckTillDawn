package io.bennyoe.components.debug

import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import io.bennyoe.widgets.BtBubbleWidget
import ktx.log.logger

class BTBubbleComponent(
    private val uiStage: Stage,
) : Component<BTBubbleComponent> {
    val bubble by lazy { BtBubbleWidget() }

    override fun World.onAdd(entity: Entity) {
        uiStage.addActor(bubble)
    }

    override fun World.onRemove(entity: Entity) {
        bubble.remove()
    }

    override fun type() = BTBubbleComponent

    companion object : ComponentType<BTBubbleComponent>() {
        val logger = logger<BTBubbleComponent>()
    }
}
