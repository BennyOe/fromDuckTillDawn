package io.bennyoe.state

import com.badlogic.gdx.graphics.g2d.Animation
import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import io.bennyoe.components.AnimationComponent
import io.bennyoe.components.AnimationType
import io.bennyoe.components.AnimationVariant
import io.bennyoe.components.PhysicComponent
import io.bennyoe.components.StateComponent

abstract class AbstractStateContext(
    val entity: Entity,
    val world: World,
    var deltaTime: Float = 0f,
) {
    val animationComponent: AnimationComponent by lazy { with(world) { entity[AnimationComponent] } }
    val stateComponent: StateComponent by lazy { with(world) { entity[StateComponent] } }
    val physicComponent: PhysicComponent by lazy { with(world) { entity[PhysicComponent] } }

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

    fun <S : AbstractFSM> changeState(state: S) {
        stateComponent.changeState(state)
    }

    fun previousState(): AbstractFSM = stateComponent.stateMachine.previousState
}
