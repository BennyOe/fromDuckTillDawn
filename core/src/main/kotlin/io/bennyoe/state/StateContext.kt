package io.bennyoe.state

import com.badlogic.gdx.ai.fsm.State
import com.badlogic.gdx.graphics.g2d.Animation
import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import io.bennyoe.components.AnimationComponent
import io.bennyoe.components.AnimationModel
import io.bennyoe.components.AnimationType
import io.bennyoe.components.AnimationVariant
import io.bennyoe.components.AttackComponent
import io.bennyoe.components.HealthComponent
import io.bennyoe.components.InputComponent
import io.bennyoe.components.JumpComponent
import io.bennyoe.components.MoveComponent
import io.bennyoe.components.PhysicComponent
import io.bennyoe.components.StateComponent

data class StateContext(
    val entity: Entity,
    val world: World,
    var deltaTime: Float = 0f,
) {
    val animationComponent: AnimationComponent
    val inputComponent: InputComponent
    val stateComponent: StateComponent
    val physicComponent: PhysicComponent
    val moveComponent: MoveComponent
    val jumpComponent: JumpComponent
    val healthComponent: HealthComponent
    val attackComponent: AttackComponent

    init {
        with(world) {
            animationComponent = entity[AnimationComponent]
            inputComponent = entity[InputComponent]
            stateComponent = entity[StateComponent]
            physicComponent = entity[PhysicComponent]
            moveComponent = entity[MoveComponent]
            jumpComponent = entity[JumpComponent]
            healthComponent = entity[HealthComponent]
            attackComponent = entity[AttackComponent]
        }
    }

    // helper methods for ECS
    inline fun <reified T : Component<T>> get(type: ComponentType<T>): T = with(world) { entity[type] }

    inline fun <reified T : Component<T>> remove(type: ComponentType<T>) = with(world) { entity.configure { it -= type } }

    inline fun <reified T : Component<T>> add(component: T) = with(world) { entity.configure { it += component } }

    fun setAnimation(
        type: AnimationType,
        playMode: Animation.PlayMode = Animation.PlayMode.LOOP,
        variant: AnimationVariant = AnimationVariant.FIRST,
        resetStateTime: Boolean = false,
        isReversed: Boolean = false,
    ) {
        animationComponent.nextAnimation(type, variant)
        if (resetStateTime) animationComponent.stateTime = 0f
        animationComponent.isReversed = isReversed
        animationComponent.mode = playMode
    }

    fun changeState(state: PlayerFSM) {
        stateComponent.changeState(state)
    }

    fun previousState(): State<StateContext> = stateComponent.stateMachine.previousState
}
