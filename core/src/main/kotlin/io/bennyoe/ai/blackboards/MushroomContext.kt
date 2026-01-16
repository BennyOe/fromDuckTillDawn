package io.bennyoe.ai.blackboards

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Circle
import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import io.bennyoe.ai.blackboards.capabilities.ChaseState
import io.bennyoe.ai.blackboards.capabilities.Chaseable
import io.bennyoe.ai.blackboards.capabilities.DefaultChaseState
import io.bennyoe.components.HealthComponent
import io.bennyoe.components.IntentionComponent
import io.bennyoe.components.PhysicComponent
import io.bennyoe.components.PlayerComponent
import io.bennyoe.components.StateComponent
import io.bennyoe.components.WalkDirection
import io.bennyoe.components.ai.BasicSensorsComponent
import io.bennyoe.components.ai.BasicSensorsHitComponent
import io.bennyoe.components.ai.BehaviorTreeComponent
import io.bennyoe.components.ai.LedgeSensorsHitComponent
import io.bennyoe.components.ai.NearbyEnemiesComponent
import io.bennyoe.components.ai.SuspicionComponent
import io.bennyoe.components.animation.AnimationComponent
import io.bennyoe.components.characterMarker.PlayerComponent
import io.bennyoe.config.EntityCategory
import io.bennyoe.systems.debug.DebugRenderer
import io.bennyoe.systems.debug.addToDebugView
import io.bennyoe.utility.EntityBodyData
import io.bennyoe.utility.SensorType.ATTACK_SENSOR
import io.bennyoe.utility.SensorType.GROUND_DETECT_SENSOR
import io.bennyoe.utility.SensorType.WALL_SENSOR
import ktx.log.logger

class MushroomContext(
    entity: Entity,
    override val world: World,
    stage: Stage,
    debugRenderer: DebugRenderer,
) : AbstractBlackboard(entity, stage, world, debugRenderer),
    ChaseState by DefaultChaseState(),
    Chaseable {
    override val intentionCmp: IntentionComponent
    override val phyCmp: PhysicComponent
    override val basicSensorsHitCmp: BasicSensorsHitComponent
    override val ledgeSensorsHitCmp: LedgeSensorsHitComponent
    override val basicSensorsCmp: BasicSensorsComponent
    override val playerEntity = world.family { all(PlayerComponent, PhysicComponent) }.first()
    override val playerPhysicCmp = with(world) { playerEntity[PhysicComponent] }
    val animCmp: AnimationComponent
    val nearbyEnemiesCmp: NearbyEnemiesComponent
    val healthCmp: HealthComponent
    val stateCmp: StateComponent<*, *>
    val suspicionCmp: SuspicionComponent

    init {
        with(world) {
            nearbyEnemiesCmp = entity[NearbyEnemiesComponent]
            phyCmp = entity[PhysicComponent]
            animCmp = entity[AnimationComponent]
            healthCmp = entity[HealthComponent]
            intentionCmp = entity[IntentionComponent]
            basicSensorsHitCmp = entity[BasicSensorsHitComponent]
            ledgeSensorsHitCmp = entity[LedgeSensorsHitComponent]
            stateCmp = entity[StateComponent]
            basicSensorsCmp = entity[BasicSensorsComponent]
            suspicionCmp = entity[SuspicionComponent]
        }
    }

    fun isAlive(): Boolean = !healthCmp.isDead

    fun isAnimationFinished(): Boolean = animCmp.isAnimationFinished()

    fun canAttack(): Boolean = basicSensorsHitCmp.getSensorHit(ATTACK_SENSOR)

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
        if (basicSensorsHitCmp.getSensorHit(WALL_SENSOR) || !basicSensorsHitCmp.getSensorHit(GROUND_DETECT_SENSOR)) {
            intentionCmp.walkDirection =
                when (intentionCmp.walkDirection) {
                    WalkDirection.LEFT -> WalkDirection.RIGHT
                    WalkDirection.RIGHT -> WalkDirection.LEFT
                    else -> WalkDirection.NONE
                }
        }
    }

    companion object {
        val logger = logger<MinotaurContext>()
        val TMP_CIRC = Circle()
    }
}
