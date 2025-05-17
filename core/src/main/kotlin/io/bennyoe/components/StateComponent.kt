package io.bennyoe.components

import com.badlogic.gdx.ai.fsm.DefaultStateMachine
import com.badlogic.gdx.ai.fsm.State
import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import io.bennyoe.state.GlobalState
import io.bennyoe.state.PlayerFSM
import io.bennyoe.state.StateContext

data class StateComponent(
    val world: World,
    var stateTime: Float = 0f,
    val stateMachine: DefaultStateMachine<StateContext, State<StateContext>> = DefaultStateMachine(),
) : Component<StateComponent> {
    override fun World.onAdd(entity: Entity) {
        stateMachine.owner = StateContext(entity, world)
        stateMachine.globalState = GlobalState.CHECK_ALIVE
        stateMachine.setInitialState(PlayerFSM.IDLE)
    }

    fun changeState(newState: State<StateContext>) {
        if (newState != stateMachine.currentState) {
            stateMachine.changeState(newState)
        }
    }

    override fun type() = StateComponent

    companion object : ComponentType<StateComponent>() {
        val logger = ktx.log.logger<StateComponent>()
    }
}
