package io.bennyoe.systems

import com.badlogic.gdx.ai.GdxAI
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import io.bennyoe.ai.GameObjectState
import io.bennyoe.components.AiComponent
import io.bennyoe.components.InputComponent

class AiSystem : IteratingSystem(family { all(AiComponent) }) {
    private val inputEntities = world.family { all(InputComponent) }

    override fun onTick() {
        GdxAI.getTimepiece().update(deltaTime)
        super.onTick()
    }

    override fun onTickEntity(entity: Entity) {
        val aiComponent = entity[AiComponent]

        aiComponent.stateTime += deltaTime
        aiComponent.stateMachine.update()

        inputEntities.forEach { inputEntity ->
            val inputComponent = inputEntity[InputComponent]
            when {
                inputComponent.jump -> {
                    aiComponent.nextStateIntent = GameObjectState.JUMP
                }

                inputComponent.crouch -> {
                    aiComponent.nextStateIntent = GameObjectState.CROUCH
                }

                else -> {
                    aiComponent.nextStateIntent = GameObjectState.IDLE
                }
            }
            aiComponent.update()
        }
    }

    companion object {
        val logger = ktx.log.logger<AiSystem>()
    }
}
