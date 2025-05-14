package io.bennyoe.components

import com.badlogic.gdx.ai.fsm.DefaultStateMachine
import com.badlogic.gdx.ai.fsm.State
import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import io.bennyoe.ai.DefaultState
import io.bennyoe.ai.PlayerFSM
import io.bennyoe.ai.StateContext

data class AiComponent(
    val world: World,
    var stateTime: Float = 0f,
    val stateMachine: DefaultStateMachine<StateContext, State<StateContext>> = DefaultStateMachine(),
) : Component<AiComponent> {
    override fun World.onAdd(entity: Entity) {
        stateMachine.owner = StateContext(entity, world)
        stateMachine.globalState = DefaultState.NONE
        stateMachine.setInitialState(PlayerFSM.IDLE)
    }

    override fun type() = AiComponent

    companion object : ComponentType<AiComponent>() {
        val logger = ktx.log.logger<AiComponent>()
    }
}
