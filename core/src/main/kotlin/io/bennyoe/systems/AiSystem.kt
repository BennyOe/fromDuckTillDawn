package io.bennyoe.systems

import com.badlogic.gdx.ai.GdxAI
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import io.bennyoe.components.AiComponent

class AiSystem : IteratingSystem(family { all(AiComponent) }) {

    override fun onTick() {
        GdxAI.getTimepiece().update(deltaTime)
        super.onTick()
    }

    override fun onTickEntity(entity: Entity) {
        val aiComponent = entity[AiComponent]

        aiComponent.stateTime += deltaTime
        aiComponent.stateMachine.update()
    }

    override fun onUpdate() {
        GdxAI.getTimepiece().update(deltaTime)
        super.onUpdate()
    }

    companion object {
        val logger = ktx.log.logger<AiSystem>()
    }
}
