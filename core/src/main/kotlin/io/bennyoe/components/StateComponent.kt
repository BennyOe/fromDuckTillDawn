package io.bennyoe.components

import com.badlogic.gdx.ai.fsm.DefaultStateMachine
import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import com.github.quillraven.fleks.World
import io.bennyoe.state.AbstractFSM
import io.bennyoe.state.AbstractStateContext

data class StateComponent<C : AbstractStateContext<C>, S : AbstractFSM<C>>(
    val world: World,
    val owner: C,
    val initialState: S,
    val globalState: S,
    var stateTime: Float = 0f,
) : Component<StateComponent<C, S>> {
    val stateMachine: DefaultStateMachine<C, S> = DefaultStateMachine(owner, initialState, globalState)

    fun changeState(newState: S) {
        if (newState != stateMachine.currentState) {
            stateMachine.changeState(newState)
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun type(): ComponentType<StateComponent<C, S>> = StateComponent as ComponentType<StateComponent<C, S>>

    companion object : ComponentType<StateComponent<*, *>>() {
        val logger = ktx.log.logger<StateComponent<*, *>>()
    }
}
