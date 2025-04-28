package io.bennyoe.systems

import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import com.github.quillraven.fleks.World.Companion.inject
import io.bennyoe.components.AiComponent
import io.bennyoe.components.PhysicComponent
import io.bennyoe.components.StateBubbleComponent
import ktx.log.logger

class StateBubbleSystem(
    val stage: Stage = inject(),
) : IteratingSystem(family { all(StateBubbleComponent) }) {
    override fun onTickEntity(entity: Entity) {
        val stateBubbleCmp = entity[StateBubbleComponent]
        val aiComponent = entity[AiComponent]
        val physicComponent = entity[PhysicComponent]
        stateBubbleCmp.bubble.displayState(aiComponent.stateMachine.currentState.toString())
        stateBubbleCmp.bubble.setPosition(physicComponent.body.position)
    }

    companion object {
        val logger = logger<StateBubbleSystem>()
    }
}
