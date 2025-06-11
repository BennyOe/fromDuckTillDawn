package io.bennyoe.ai.blackboards

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import io.bennyoe.components.AnimationComponent
import io.bennyoe.components.HealthComponent
import io.bennyoe.components.IntentionComponent
import io.bennyoe.components.PhysicComponent
import io.bennyoe.components.PlayerComponent
import io.bennyoe.components.StateComponent
import io.bennyoe.components.WalkDirection
import io.bennyoe.components.ai.BehaviorTreeComponent
import io.bennyoe.components.ai.NearbyEnemiesComponent
import io.bennyoe.components.ai.RayHitComponent
import io.bennyoe.service.DebugRenderService
import io.bennyoe.service.addToDebugView
import io.bennyoe.state.mushroom.MushroomFSM
import io.bennyoe.state.mushroom.MushroomStateContext
import io.bennyoe.state.player.PlayerFSM
import ktx.log.logger
import ktx.math.compareTo
import ktx.math.component1
import ktx.math.component2

class MushroomContext(
    entity: Entity,
    world: World,
    stage: Stage,
    val debugRenderService: DebugRenderService,
) : AbstractBlackboard(entity, world, stage) {
    val nearbyEnemiesCmp: NearbyEnemiesComponent
    val phyCmp: PhysicComponent
    val animCmp: AnimationComponent
    val intentionCmp: IntentionComponent
    val rayHitCmp: RayHitComponent
    val healthCmp: HealthComponent
    val stateCmp: StateComponent<*, *>
    val location: Vector2
        get() = phyCmp.body.position

    init {
        with(world) {
            nearbyEnemiesCmp = entity[NearbyEnemiesComponent]
            phyCmp = entity[PhysicComponent]
            animCmp = entity[AnimationComponent]
            healthCmp = entity[HealthComponent]
            intentionCmp = entity[IntentionComponent]
            rayHitCmp = entity[RayHitComponent]
            stateCmp = entity[StateComponent]
        }
    }

    // TODO implement all the methods for walking, which contains
    // check raycast for hitting wall
    // check raycast for gap in ground
    // reverse the walking direction
    fun reverseWalkingDirection(direction: WalkDirection): WalkDirection {
        if (rayHitCmp.wallHit || (!rayHitCmp.jumpHit && !rayHitCmp.groundHit && stateCmp.stateMachine.currentState != MushroomFSM.FALL)) {
            return when (direction) {
                WalkDirection.LEFT -> WalkDirection.RIGHT
                WalkDirection.RIGHT -> WalkDirection.LEFT
                else -> WalkDirection.NONE
            }
        }
        return direction
    }

    // TODO implement all the methods for attack, which contains
    // check if in detection range
    // walk to player until in attack range
    // follow player (also facing direction)
    // start attack
    // have a 20% chance of dodging a players attack

    // TODO implement all the methods for jumping, which contains
    // detect raycast for hitting a wall
    // check raycast if platform is jumpable
    // jump

    fun moveTo() {
        if (!rayHitCmp.groundHit && rayHitCmp.jumpHit && stateCmp.stateMachine.currentState != PlayerFSM.DOUBLE_JUMP) {
            intentionCmp.wantsToJump = true
        } else {
            intentionCmp.walkDirection = reverseWalkingDirection(intentionCmp.walkDirection)
        }
    }

    fun isAlive(): Boolean = !healthCmp.isDead

    fun inRange(
        range: Float,
        targetPos: Vector2,
    ): Boolean {
        val (sourceX, sourceY) = phyCmp.body.position
        val (sourceOffX, sourceOffY) = phyCmp.offset
        var (sourceSizeX, sourceSizeY) = phyCmp.size
        sourceSizeX += range
        sourceSizeY += range

        TMP_RECT
            .set(
                sourceOffX + sourceX - sourceSizeX * 0.5f,
                sourceOffY + sourceY - sourceSizeY * 0.5f,
                sourceSizeX,
                sourceSizeY,
            ).addToDebugView(debugRenderService, Color.BLACK, "range")
        return TMP_RECT.contains(targetPos)
    }

    fun stopMovement() {
        intentionCmp.walkDirection = WalkDirection.NONE
    }

    // TODO implement
    // check if enemy is in attack range
    // check if facing direction is where the enemy is
    fun canAttack(): Boolean = true

    fun hasEnemyNearby(): Boolean {
        with(world) {
            nearbyEnemiesCmp.target = nearbyEnemiesCmp.nearbyEntities
                .firstOrNull {
                    it has PlayerComponent.Companion
                } ?: BehaviorTreeComponent.Companion.NO_TARGET
        }
        return nearbyEnemiesCmp.target != BehaviorTreeComponent.Companion.NO_TARGET
    }

    fun isAnimationFinished(): Boolean = animCmp.isAnimationFinished()

    // TODO implement
    fun startAttack() {
        intentionCmp.wantsToAttack = true
    }

    fun stopAttack() {
        intentionCmp.wantsToAttack = false
    }

    companion object {
        val logger = logger<MushroomContext>()
        val TMP_RECT = Rectangle()
    }
}
