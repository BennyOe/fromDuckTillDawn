package io.bennyoe.state

import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import io.bennyoe.components.DeadComponent
import io.bennyoe.components.HealthComponent
import io.bennyoe.components.IntentionComponent
import io.bennyoe.components.JumpComponent
import io.bennyoe.components.MoveComponent
import io.bennyoe.components.PhysicComponent
import io.bennyoe.components.StateComponent
import io.bennyoe.components.WalkDirection
import io.bennyoe.components.animation.AnimationComponent
import io.bennyoe.components.animation.AnimationKey

abstract class AbstractStateContext<C : AbstractStateContext<C>>(
    val entity: Entity,
    val world: World,
    val stage: Stage,
    var deltaTime: Float = 0f,
) {
    // this is needed to prevent flickering of the death animation
    var deathAlreadyEnteredBefore = false

    // this cast has to be made because of the type erasure of fleks
    @Suppress("UNCHECKED_CAST")
    val stateComponent: StateComponent<C, *> by lazy { with(world) { entity[StateComponent] as StateComponent<C, *> } }
    val animationComponent: AnimationComponent by lazy { with(world) { entity[AnimationComponent] } }
    val physicComponent: PhysicComponent by lazy { with(world) { entity[PhysicComponent] } }
    val moveComponent: MoveComponent by lazy { with(world) { entity[MoveComponent] } }
    val jumpComponent: JumpComponent by lazy { with(world) { entity[JumpComponent] } }
    val healthComponent: HealthComponent by lazy { with(world) { entity[HealthComponent] } }
    val intentionComponent: IntentionComponent by lazy { with(world) { entity[IntentionComponent] } }
    val deadComponent: DeadComponent by lazy { with(world) { entity[DeadComponent] } }

    abstract val wantsToJump: Boolean
    abstract val wantsToAttack: Boolean

    val wantsToWalk get() = intentionComponent.walkDirection != WalkDirection.NONE
    val wantsToIdle get() = intentionComponent.walkDirection == WalkDirection.NONE

    // helper methods for ECS
    inline fun <reified T : Component<T>> get(type: ComponentType<T>): T = with(world) { entity[type] }

    inline fun <reified T : Component<T>> remove(type: ComponentType<T>) = with(world) { entity.configure { it -= type } }

    inline fun <reified T : Component<T>> add(component: T) = with(world) { entity.configure { it += component } }

    open fun entityIsDead(
        keepCorpse: Boolean,
        removeDelay: Float,
    ) {
        moveComponent.lockMovement = true
        stateComponent.stateMachine.globalState = null
        moveComponent.moveVelocity.x = 0f
        deathAlreadyEnteredBefore = true
    }

    fun setAnimation(
        type: AnimationKey,
        playMode: Animation.PlayMode = Animation.PlayMode.LOOP,
        resetStateTime: Boolean = false,
        isReversed: Boolean = false,
    ) {
        animationComponent.nextAnimation(type)
        if (resetStateTime) animationComponent.stateTime = 0f
        animationComponent.isReversed = isReversed
        animationComponent.mode = playMode
    }

    @Suppress("UNCHECKED_CAST")
    fun <S : AbstractFSM<C>> changeState(state: S) {
        (stateComponent as StateComponent<C, S>).changeState(state)
    }

    @Suppress("UNCHECKED_CAST")
    fun <S : AbstractFSM<C>> previousState() {
        changeState((stateComponent as StateComponent<C, S>).stateMachine.previousState)
    }

    @Suppress("UNCHECKED_CAST")
    fun <S : AbstractFSM<C>> setGlobalState(state: S) {
        (stateComponent as StateComponent<C, S>).stateMachine.globalState = state
    }

    fun previousState(): AbstractFSM<C> = stateComponent.stateMachine.previousState
}
