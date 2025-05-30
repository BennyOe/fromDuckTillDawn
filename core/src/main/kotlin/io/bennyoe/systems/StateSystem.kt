package io.bennyoe.systems

import com.badlogic.gdx.ai.GdxAI
import com.badlogic.gdx.ai.msg.MessageManager
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import io.bennyoe.components.StateComponent

class StateSystem : IteratingSystem(family { all(StateComponent) }) {
    private val messageDispatcher = MessageManager.getInstance()

    override fun onTick() {
        GdxAI.getTimepiece().update(deltaTime)
        messageDispatcher.update()
        super.onTick()
    }

    override fun onTickEntity(entity: Entity) {
        val stateComponent = entity[StateComponent]

        stateComponent.stateTime += deltaTime
        stateComponent.stateMachine.owner.deltaTime = deltaTime
        stateComponent.stateMachine.update()
    }

    companion object {
        val logger = ktx.log.logger<StateSystem>()
    }
}
