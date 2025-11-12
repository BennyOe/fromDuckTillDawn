package io.bennyoe.state.minotaur

import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import io.bennyoe.components.AttackComponent
import io.bennyoe.components.ImageComponent
import io.bennyoe.components.IntentionComponent
import io.bennyoe.components.PhysicComponent
import io.bennyoe.components.PlayerComponent
import io.bennyoe.components.TransformComponent
import io.bennyoe.components.WalkDirection
import io.bennyoe.components.ai.RayHitComponent
import io.bennyoe.state.AbstractStateContext

class MinotaurStateContext(
    entity: Entity,
    world: World,
    stage: Stage,
    deltaTime: Float = 0f,
) : AbstractStateContext<MinotaurStateContext>(entity, world, stage, deltaTime) {
    val intentionCmp: IntentionComponent by lazy { with(world) { entity[IntentionComponent] } }
    val attackCmp: AttackComponent by lazy { with(world) { entity[AttackComponent] } }
    val imageCmp: ImageComponent by lazy { with(world) { entity[ImageComponent] } }
    val playerEntity = world.family { all(PlayerComponent, PhysicComponent) }.first()
    val rayHitCmp: RayHitComponent by lazy { with(world) { entity[RayHitComponent] } }

    override val wantsToJump get() = intentionCmp.wantsToJump
    override val wantsToAttack get() = intentionCmp.wantsToAttack
    val wantsToScream get() = intentionCmp.wantsToScream
    val wantsToGrabAttack get() = intentionCmp.wantsToGrabAttack
    val wantsToThrowAttack get() = intentionCmp.wantsToThrowAttack
    val wantsToStompAttack get() = intentionCmp.wantsToStomp

    fun rotateToPlayer() {
        val playerTransformCmp = with(world) { playerEntity[TransformComponent] }
        imageCmp.flipImage = if (transformCmp.position.x < playerTransformCmp.position.x) false else true
    }

    fun runTowardsPlayer() {
        val playerTransformCmp = with(world) { playerEntity[TransformComponent] }
        val direction = if (playerTransformCmp.position.x < transformCmp.position.x) WalkDirection.LEFT else WalkDirection.RIGHT
        intentionCmp.walkDirection = direction
        intentionCmp.wantsToChase = true
    }

    fun stopMovement() {
        intentionCmp.walkDirection = WalkDirection.NONE
    }

    fun runIntoWall(): Boolean = rayHitCmp.wallHit

    fun resetAllIntentions() {
        intentionCmp.resetAllIntentions()
    }

    fun runIntoPlayer(): Boolean = rayHitCmp.canAttack
}
