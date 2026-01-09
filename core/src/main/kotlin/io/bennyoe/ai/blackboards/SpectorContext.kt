package io.bennyoe.ai.blackboards

import com.badlogic.gdx.math.Circle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import io.bennyoe.ai.blackboards.SpectorContext.SpectorAwareness
import io.bennyoe.components.HealthComponent
import io.bennyoe.components.IntentionComponent
import io.bennyoe.components.PhysicComponent
import io.bennyoe.components.PlayerComponent
import io.bennyoe.components.StateComponent
import io.bennyoe.components.WalkDirection
import io.bennyoe.components.ai.BasicSensorsComponent
import io.bennyoe.components.ai.BasicSensorsHitComponent
import io.bennyoe.components.ai.LedgeSensorsHitComponent
import io.bennyoe.components.ai.NearbyEnemiesComponent
import io.bennyoe.components.ai.SuspicionComponent
import io.bennyoe.components.animation.AnimationComponent
import io.bennyoe.systems.debug.DebugRenderer
import io.bennyoe.systems.debug.addToDebugView
import io.bennyoe.utility.SensorType.ATTACK_SENSOR
import io.bennyoe.utility.SensorType.GROUND_DETECT_SENSOR
import io.bennyoe.utility.SensorType.WALL_SENSOR
import ktx.log.logger
import ktx.math.vec2
import kotlin.math.abs

private const val INVESTIGATION_THRESHOLD = 0.2f
private const val SEARCH_THRESHOLD = 0.4f
private const val CHASE_THRESHOLD = 0.6f

class SpectorContext(
    entity: Entity,
    world: World,
    stage: Stage,
    debugRenderer: DebugRenderer,
) : AbstractBlackboard(entity, world, stage, debugRenderer),
    HasAwareness<SpectorAwareness> {
    val nearbyEnemiesCmp: NearbyEnemiesComponent
    val phyCmp: PhysicComponent
    val animCmp: AnimationComponent
    val intentionCmp: IntentionComponent
    val basicSensorsHitCmp: BasicSensorsHitComponent
    val ledgeSensorsHitCmp: LedgeSensorsHitComponent
    val healthCmp: HealthComponent
    val stateCmp: StateComponent<*, *>
    val basicSensorsCmp: BasicSensorsComponent
    val playerEntity = world.family { all(PlayerComponent, PhysicComponent) }.first()
    val playerPhysicCmp = with(world) { playerEntity[PhysicComponent] }
    val suspicionCmp: SuspicionComponent
    var searchIsFinished: Boolean = true
    var investigationIsFinished: Boolean = true

    override var awareness = SpectorAwareness.CALM

    enum class SpectorAwareness {
        CALM,
        INVESTIGATING,
        SEARCHING,
        CHASING,
    }

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

    override fun updateAwareness() {
        val suspicion = suspicionCmp.combinedSuspicionStrength

        when (awareness) {
            SpectorAwareness.CALM -> {
                when {
                    suspicion > CHASE_THRESHOLD -> {
                        awareness = SpectorAwareness.CHASING
                    }

                    suspicion > SEARCH_THRESHOLD -> {
                        awareness = SpectorAwareness.SEARCHING
                        searchIsFinished = false
                    }

                    suspicion > INVESTIGATION_THRESHOLD -> {
                        investigationIsFinished = false
                        awareness = SpectorAwareness.INVESTIGATING
                    }
                }
            }

            SpectorAwareness.INVESTIGATING -> {
                when {
                    suspicion > CHASE_THRESHOLD -> {
                        awareness = SpectorAwareness.CHASING
                    }

                    suspicion > SEARCH_THRESHOLD -> {
                        awareness = SpectorAwareness.SEARCHING
                        searchIsFinished = false
                    }

                    suspicion < INVESTIGATION_THRESHOLD && investigationIsFinished -> {
                        awareness = SpectorAwareness.CALM
                    }
                }
            }

            SpectorAwareness.SEARCHING -> {
                when {
                    suspicion > CHASE_THRESHOLD -> {
                        awareness = SpectorAwareness.CHASING
                    }

                    suspicion < SEARCH_THRESHOLD && searchIsFinished -> {
                        awareness = SpectorAwareness.CALM
                    }
                }
            }

            SpectorAwareness.CHASING -> {
                when {
                    suspicion < SEARCH_THRESHOLD -> {
                        awareness = SpectorAwareness.SEARCHING
                        searchIsFinished = false
                    }
                }
            }
        }
    }

    fun getPositionToGoTo(target: String): Vector2 {
        logger.debug { "Moving to position: ${suspicionCmp.lastKnownPlayerPos}" }
        return when (target) {
            "lastKnownPos" -> suspicionCmp.lastKnownPlayerPos
            "player" -> playerPhysicCmp.body.position
            else -> vec2(0f, 0f)
        }!!
    }

    fun moveToPosition(pos: Vector2): Boolean {
        logger.debug { "POSITION DIFFERENCE ====== ${abs(pos.x - phyCmp.body.position.x)}" }
        // MODIFIED! If we are blocked by a wall or would walk off a ledge, stop and treat as "arrived"
        // so sequences (investigate/search) can finish instead of getting stuck forever.
        Circle(pos.x, pos.y, 0.2f).addToDebugView(service = debugRenderer, label = "lastKnownPos")
        if (basicSensorsHitCmp.getSensorHit(WALL_SENSOR) || !basicSensorsHitCmp.getSensorHit(GROUND_DETECT_SENSOR)) {
            intentionCmp.walkDirection = WalkDirection.NONE
            return true
        }
        if (abs(pos.x - phyCmp.body.position.x) < 0.5f) {
            intentionCmp.walkDirection = WalkDirection.NONE
            return true
        }

        if (pos.x < phyCmp.body.position.x) {
            intentionCmp.walkDirection = WalkDirection.LEFT
        }
        if (pos.x > phyCmp.body.position.x) {
            intentionCmp.walkDirection = WalkDirection.RIGHT
        }
        return false
    }

    fun isAlive(): Boolean = !healthCmp.isDead

    fun isAnimationFinished(): Boolean = animCmp.isAnimationFinished()

    fun canAttack(): Boolean = basicSensorsHitCmp.getSensorHit(ATTACK_SENSOR)

    fun stopMovement() {
        intentionCmp.walkDirection = WalkDirection.NONE
    }

    fun idle() {
        stopMovement()
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

    fun isRelaxed(): Boolean = awareness == SpectorAwareness.CALM

    fun isIrritated(): Boolean = awareness == SpectorAwareness.INVESTIGATING

    fun isSuspicious(): Boolean = awareness == SpectorAwareness.SEARCHING

    fun hasIdentified(): Boolean = awareness == SpectorAwareness.CHASING

    companion object {
        val logger = logger<SpectorContext>()
    }
}
