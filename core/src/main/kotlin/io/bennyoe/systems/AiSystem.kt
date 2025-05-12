package io.bennyoe.systems

import com.badlogic.gdx.ai.GdxAI
import com.badlogic.gdx.ai.msg.MessageManager
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import io.bennyoe.components.AiComponent

class AiSystem : IteratingSystem(family { all(AiComponent) }) {
    private val messageDispatcher = MessageManager.getInstance()

    override fun onTick() {
        GdxAI.getTimepiece().update(deltaTime)
        messageDispatcher.update()
        super.onTick()
    }

    override fun onTickEntity(entity: Entity) {
        val aiComponent = entity[AiComponent]

        aiComponent.stateTime += deltaTime
        aiComponent.stateMachine.owner.deltaTime = deltaTime
        aiComponent.stateMachine.update()
    }

    companion object {
        val logger = ktx.log.logger<AiSystem>()
    }
}
