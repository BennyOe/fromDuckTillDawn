package io.bennyoe.ai

import com.badlogic.gdx.graphics.g2d.Animation
import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import io.bennyoe.components.AiComponent
import io.bennyoe.components.AnimationComponent
import io.bennyoe.components.AnimationModel
import io.bennyoe.components.AnimationType
import io.bennyoe.components.AnimationVariant
import io.bennyoe.components.InputComponent
import io.bennyoe.components.JumpComponent
import io.bennyoe.components.MoveComponent
import io.bennyoe.components.PhysicComponent

data class StateContext(
    val entity: Entity,
    val world: World,
) {
    val animationComponent: AnimationComponent
    val inputComponent: InputComponent
    val aiComponent: AiComponent
    val physicComponent: PhysicComponent
    val moveComponent: MoveComponent
    val jumpComponent: JumpComponent

    init {
        with(world) {
            animationComponent = entity[AnimationComponent]
            inputComponent = entity[InputComponent]
            aiComponent = entity[AiComponent]
            physicComponent = entity[PhysicComponent]
            moveComponent = entity[MoveComponent]
            jumpComponent = entity[JumpComponent]
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
    ) {
        animationComponent.nextAnimation(AnimationModel.PLAYER_DAWN, type, variant)
        animationComponent.animation.playMode = playMode
    }

    fun changeState(state: PlayerFSM) {
        aiComponent.stateMachine.changeState(state)
    }

    fun previousState(): PlayerFSM = aiComponent.stateMachine.previousState
}
