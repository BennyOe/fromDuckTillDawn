package io.bennyoe.ai.blackboards

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Circle
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
import io.bennyoe.components.ai.BasicSensorsComponent
import io.bennyoe.components.ai.BehaviorTreeComponent
import io.bennyoe.components.ai.NearbyEnemiesComponent
import io.bennyoe.components.ai.RayHitComponent
import io.bennyoe.config.EntityCategory
import io.bennyoe.systems.debug.DebugRenderer
import io.bennyoe.systems.debug.addToDebugView
import io.bennyoe.utility.EntityBodyData
import ktx.log.logger
import kotlin.math.abs

class MinotaurContext(
    entity: Entity,
    world: World,
    stage: Stage,
    debugRenderer: DebugRenderer,
) : AbstractBlackboard(entity, world, stage, debugRenderer) {
    val nearbyEnemiesCmp: NearbyEnemiesComponent
    val phyCmp: PhysicComponent
    val animCmp: AnimationComponent
    val intentionCmp: IntentionComponent
    val rayHitCmp: RayHitComponent
    val healthCmp: HealthComponent
    val stateCmp: StateComponent<*, *>
    val basicSensorsCmp: BasicSensorsComponent
    val playerEntity = world.family { all(PlayerComponent, PhysicComponent) }.first()

    init {
        with(world) {
            nearbyEnemiesCmp = entity[NearbyEnemiesComponent]
            phyCmp = entity[PhysicComponent]
            animCmp = entity[AnimationComponent]
            healthCmp = entity[HealthComponent]
            intentionCmp = entity[IntentionComponent]
            rayHitCmp = entity[RayHitComponent]
            stateCmp = entity[StateComponent]
            basicSensorsCmp = entity[BasicSensorsComponent]
        }
    }

    fun isAlive(): Boolean = !healthCmp.isDead

    fun isAnimationFinished(): Boolean = animCmp.isAnimationFinished()

    fun canAttack(): Boolean = rayHitCmp.canAttack

    fun hasPlayerNearby(): Boolean {
        with(world) {
            nearbyEnemiesCmp.target = nearbyEnemiesCmp.nearbyEntities
                .firstOrNull {
                    val bodyData = it[PhysicComponent].body.userData as EntityBodyData
                    bodyData.entityCategory == EntityCategory.PLAYER
                } ?: BehaviorTreeComponent.NO_TARGET
        }
        return nearbyEnemiesCmp.target != BehaviorTreeComponent.NO_TARGET
    }

    fun isPlayerInChaseRange(): Boolean {
        val selfPos = phyCmp.body.position

        // draw the chase range
        TMP_CIRC
            .set(
                phyCmp.body.position.x,
                phyCmp.body.position.y,
                basicSensorsCmp.chaseRange,
            )
        TMP_CIRC.addToDebugView(debugRenderer, Color.GREEN, "chaseRange")

        // calculate the distance to the player and return true if it is < chaseRange
        val player = world.family { all(PlayerComponent, PhysicComponent) }.firstOrNull() ?: return false
        val playerPos = with(world) { player[PhysicComponent].body.position }
        val dist2 = selfPos.dst2(playerPos)

        return dist2 <= basicSensorsCmp.chaseRange * basicSensorsCmp.chaseRange
    }

    fun stopMovement() {
        intentionCmp.walkDirection = WalkDirection.NONE
    }

    fun startAttack() {
        intentionCmp.wantsToAttack = true
    }

    fun stopAttack() {
        intentionCmp.wantsToAttack = false
    }

    fun idle() {
        stopMovement()
        stopAttack()
    }

    fun patrol() {
        if (rayHitCmp.wallHit || !rayHitCmp.groundHit) {
            intentionCmp.walkDirection =
                when (intentionCmp.walkDirection) {
                    WalkDirection.LEFT -> WalkDirection.RIGHT
                    WalkDirection.RIGHT -> WalkDirection.LEFT
                    else -> WalkDirection.NONE
                }
        }
    }

    fun chasePlayer() {
        walkToPosition()
    }

    private fun walkToPosition() {
        val selfPos = phyCmp.body.position
        val playerPos = with(world) { playerEntity[PhysicComponent].body.position }
        val goToPosition: Float = playerPos.x
        val dist = selfPos.x - goToPosition

        when {
            abs(dist) > X_THRESHOLD -> {
                intentionCmp.walkDirection = if (dist < 0f) WalkDirection.RIGHT else WalkDirection.LEFT
            }

            else -> intentionCmp.walkDirection = WalkDirection.NONE
        }
    }

    companion object {
        val logger = logger<MinotaurContext>()
        val TMP_CIRC = Circle()
    }
}
