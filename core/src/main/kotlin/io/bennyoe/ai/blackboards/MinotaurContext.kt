package io.bennyoe.ai.blackboards

import com.badlogic.gdx.math.Circle
import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import io.bennyoe.components.HasGroundContact
import io.bennyoe.components.HealthComponent
import io.bennyoe.components.IntentionComponent
import io.bennyoe.components.PhysicComponent
import io.bennyoe.components.PlayerComponent
import io.bennyoe.components.StateComponent
import io.bennyoe.components.TransformComponent
import io.bennyoe.components.WalkDirection
import io.bennyoe.components.ai.BasicSensorsComponent
import io.bennyoe.components.ai.NearbyEnemiesComponent
import io.bennyoe.components.ai.BasicSensorsHitComponent
import io.bennyoe.components.animation.AnimationComponent
import io.bennyoe.state.minotaur.MinotaurFSM
import io.bennyoe.systems.debug.DebugRenderer
import ktx.log.logger
import kotlin.math.abs

private const val HEIGHT_DIFF_EPS = 0.2f
private const val STOMP_ATTACK_RANGE = 12f

class MinotaurContext(
    entity: Entity,
    world: World,
    stage: Stage,
    debugRenderer: DebugRenderer,
) : AbstractBlackboard(entity, world, stage, debugRenderer) {
    val nearbyEnemiesCmp: NearbyEnemiesComponent
    val phyCmp: PhysicComponent
    val transformCmp: TransformComponent
    val animCmp: AnimationComponent
    val intentionCmp: IntentionComponent
    val rayHitCmp: BasicSensorsHitComponent
    val healthCmp: HealthComponent
    val stateCmp: StateComponent<*, *>
    val basicSensorsCmp: BasicSensorsComponent
    val playerEntity = world.family { all(PlayerComponent, PhysicComponent) }.first()

    init {
        with(world) {
            nearbyEnemiesCmp = entity[NearbyEnemiesComponent]
            phyCmp = entity[PhysicComponent]
            transformCmp = entity[TransformComponent]
            animCmp = entity[AnimationComponent]
            healthCmp = entity[HealthComponent]
            intentionCmp = entity[IntentionComponent]
            rayHitCmp = entity[BasicSensorsHitComponent]
            stateCmp = entity[StateComponent]
            basicSensorsCmp = entity[BasicSensorsComponent]
        }
    }

    fun isAlive(): Boolean = !healthCmp.isDead

    fun isAnimationFinished(): Boolean = animCmp.isAnimationFinished()

    fun canAttack(): Boolean = rayHitCmp.canAttack

    fun stopMovement() {
        intentionCmp.walkDirection = WalkDirection.NONE
    }

    fun startGrabAttack() {
        intentionCmp.wantsToGrabAttack = true
    }

    inline fun <reified T : MinotaurFSM> fsmStateIsNot(): Boolean = stateCmp.stateMachine.currentState !is T

    inline fun <reified T : MinotaurFSM> fsmStateIs(): Boolean = stateCmp.stateMachine.currentState is T

    inline fun <reified T : MinotaurFSM> fsmPreviousStateIsNot(): Boolean = stateCmp.stateMachine.previousState !is T

    fun stopAttack() {
        intentionCmp.wantsToAttack = false
    }

    fun idle() {
        stopMovement()
        stopAttack()
    }

    fun seesPlayer(): Boolean = rayHitCmp.seesPlayer && !with(world) { playerEntity[HealthComponent].isDead }

    fun playerInGrabRange(): Boolean {
        val playerPhysicCmp = with(world) { playerEntity[PhysicComponent] }
        val playerBottomLine = playerPhysicCmp.body.position.y + playerPhysicCmp.offset.y - playerPhysicCmp.size.y * 0.5f
        val myBottom = phyCmp.body.position.y + phyCmp.offset.y - phyCmp.size.y * 0.5f
        return seesPlayer() && abs(playerBottomLine - myBottom) < HEIGHT_DIFF_EPS
    }

    fun playerIsInAir(): Boolean {
        val playerPhysicCmp = with(world) { playerEntity[PhysicComponent] }
        val playerBottomLine = playerPhysicCmp.body.position.y + playerPhysicCmp.offset.y - playerPhysicCmp.size.y * 0.5f
        val playerHasGroundContact = with(world) { playerEntity has HasGroundContact }

        val myBottom = phyCmp.body.position.y + phyCmp.offset.y - phyCmp.size.y * 0.5f
        return playerBottomLine > myBottom && !playerHasGroundContact
    }

    fun playerInThrowRange(): Boolean {
        logger.debug { "CHECKING FOR PLAYER IN THROW RANGE" }
        return rayHitCmp.playerInThrowRange
    }

    fun startThrowAttack() {
        intentionCmp.wantsToThrowAttack = true
    }

    fun playerInStompAttackRange(): Boolean {
        val playerXPos = with(world) { playerEntity[TransformComponent] }.position.x
        return abs(playerXPos - transformCmp.position.x) < STOMP_ATTACK_RANGE
    }

    fun startStompAttack() {
        intentionCmp.wantsToStomp = true
    }

    companion object {
        val logger = logger<MinotaurContext>()
        val TMP_CIRC = Circle()
    }
}
