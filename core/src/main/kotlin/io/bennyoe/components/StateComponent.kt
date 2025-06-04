package io.bennyoe.components

import com.badlogic.gdx.ai.fsm.DefaultStateMachine
import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import io.bennyoe.state.AbstractFSM
import io.bennyoe.state.AbstractStateContext
import io.bennyoe.state.player.PlayerCheckAliveState

data class StateComponent(
    val world: World,
    val owner: AbstractStateContext,
    val initialState: AbstractFSM,
    var stateTime: Float = 0f,
) : Component<StateComponent> {
    lateinit var stateMachine: DefaultStateMachine<AbstractStateContext, AbstractFSM>

    override fun World.onAdd(entity: Entity) {
        stateMachine = DefaultStateMachine(owner, initialState)
        stateMachine.globalState = PlayerCheckAliveState
    }

    fun changeState(newState: AbstractFSM) {
        if (newState != stateMachine.currentState) {
            stateMachine.changeState(newState)
        }
    }

    override fun type() = StateComponent

    companion object : ComponentType<StateComponent>() {
        val logger = ktx.log.logger<StateComponent>()
    }
}
