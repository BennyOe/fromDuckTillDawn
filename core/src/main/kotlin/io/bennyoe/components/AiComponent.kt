package io.bennyoe.components

import com.badlogic.gdx.ai.fsm.DefaultStateMachine
import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import io.bennyoe.ai.GameObjectState

data class AiComponent(
    val world: World,
    var stateTime: Float = 0f,
    var nextStateIntent: GameObjectState = GameObjectState.IDLE,
    val stateMachine: DefaultStateMachine<StateContext, GameObjectState> = DefaultStateMachine(),
) : Component<AiComponent> {
    lateinit var context: StateContext

    override fun World.onAdd(entity: Entity) {
        stateMachine.owner = context
        stateMachine.setInitialState(nextStateIntent)
    }

    override fun type() = AiComponent

    fun update() {
        stateMachine.update()
    }

    companion object : ComponentType<AiComponent>() {
        val logger = ktx.log.logger<AiComponent>()
    }
}

data class StateContext(
    val animationComponent: AnimationComponent,
    val physicComponent: PhysicComponent,
    val moveComponent: MoveComponent,
    val aiComponent: AiComponent,
)
