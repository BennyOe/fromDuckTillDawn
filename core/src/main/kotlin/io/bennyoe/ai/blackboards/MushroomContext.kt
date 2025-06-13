package io.bennyoe.ai.blackboards

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Circle
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import io.bennyoe.components.AnimationComponent
import io.bennyoe.components.HasGroundContact
import io.bennyoe.components.HealthComponent
import io.bennyoe.components.IntentionComponent
import io.bennyoe.components.PhysicComponent
import io.bennyoe.components.PlayerComponent
import io.bennyoe.components.StateComponent
import io.bennyoe.components.WalkDirection
import io.bennyoe.components.ai.BasicSensorsComponent
import io.bennyoe.components.ai.BehaviorTreeComponent
import io.bennyoe.components.ai.LedgeHitData
import io.bennyoe.components.ai.NearbyEnemiesComponent
import io.bennyoe.components.ai.RayHitComponent
import io.bennyoe.service.DebugRenderService
import io.bennyoe.service.addToDebugView
import ktx.collections.GdxArray
import ktx.log.logger
import ktx.math.component1
import ktx.math.component2
import kotlin.math.abs

const val Y_THRESHOLD = 0.1f
const val X_THRESHOLD = 0.1f

private const val LEDGE_OFFSET = 1f

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
    val basicSensorsCmp: BasicSensorsComponent
    var nearestPlatformLedge: Float? = null
    var platformRelation: PlatformRelation = PlatformRelation.SAME
    val playerEntity = world.family { all(PlayerComponent, PhysicComponent) }.first()
    val playerPhysicCmp = with(world) { playerEntity[PhysicComponent] }

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

    fun patrol() {
        intentionCmp.wantsToChase = false
        if (rayHitCmp.wallHit || !rayHitCmp.groundHit) {
            intentionCmp.walkDirection =
                when (intentionCmp.walkDirection) {
                    WalkDirection.LEFT -> WalkDirection.RIGHT
                    WalkDirection.RIGHT -> WalkDirection.LEFT
                    else -> WalkDirection.NONE
                }
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
        intentionCmp.wantsToChase = false
        intentionCmp.walkDirection = WalkDirection.NONE
    }

    fun canAttack(): Boolean = rayHitCmp.canAttack

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

    fun startAttack() {
        intentionCmp.wantsToChase = false
        intentionCmp.wantsToAttack = true
    }

    fun stopAttack() {
        intentionCmp.wantsToAttack = false
    }

    fun chasePlayer() {
        val playerPos = with(world) { playerEntity[PhysicComponent].body.position }
        intentionCmp.wantsToChase = true

        jumpOverWall()
        jumpOverGap()
        // check if player y > self.y
        platformRelation = heightRelationToPlayer(phyCmp, playerPhysicCmp)
        if (platformRelation == PlatformRelation.SAME) {
            nearestPlatformLedge = null
        }

        // enemy is below player
        if (platformRelation == PlatformRelation.BELOW && rayHitCmp.sightIsBlocked && intentionCmp.walkDirection == WalkDirection.NONE) {
            // check snapshot of upperLedgeSensors and find world coordinate where the nearest doesn't hit
            if (nearestPlatformLedge == null) {
                nearestPlatformLedge = findLedgeToJumpUp(rayHitCmp.upperLedgeHits, rayHitCmp.lowerLedgeHits, playerPos.x)
            }
        }

        // enemy is above player
        if (platformRelation == PlatformRelation.ABOVE && rayHitCmp.sightIsBlocked && intentionCmp.walkDirection == WalkDirection.NONE) {
            if (nearestPlatformLedge == null) {
                nearestPlatformLedge = findLedgeToDropDown(rayHitCmp.lowerLedgeHits, playerPos.x)
            }
        }

        walkToPosition()

        // jump if player is on platform above
        if (intentionCmp.walkDirection == WalkDirection.NONE && platformRelation == PlatformRelation.BELOW) {
            intentionCmp.wantsToJump = true
        }
    }

    private fun walkToPosition() {
        val selfPos = phyCmp.body.position
        val playerPos = with(world) { playerEntity[PhysicComponent].body.position }
        val goToPosition: Float = nearestPlatformLedge ?: playerPos.x
        val dist = selfPos.x - goToPosition

        when {
            abs(dist) > X_THRESHOLD -> {
                intentionCmp.walkDirection = if (dist < 0f) WalkDirection.RIGHT else WalkDirection.LEFT
            }

            else -> intentionCmp.walkDirection = WalkDirection.NONE
        }
    }

    // get the sensor nearest to the player and search to both sides until a upperLedgeSensor doesn't hit. If the lowerLedgeSensor hits this is
    // the ledge to jump to the above platform
    fun findLedgeToJumpUp(
        upperLedgeHits: GdxArray<LedgeHitData>,
        lowerLedgeHits: GdxArray<LedgeHitData>,
        playerX: Float,
    ): Float? {
        // get sensor closest to player
        val playerIndex =
            upperLedgeHits
                .minByOrNull { hit ->
                    abs(hit.xCoordinate - playerX)
                }?.let { hit -> upperLedgeHits.indexOf(hit) } ?: return null

        // iterate in both directions from this sensor to find the closest with not upper hit and a bottom hit
        for (offset in 0 until upperLedgeHits.size) {
            val left = playerIndex - offset
            val right = playerIndex + offset

            if (left >= 0 && !upperLedgeHits[left].hit && lowerLedgeHits[left].hit) {
                // add an extra offset to ensure there is enough space to jump up to the next platform
                return if (upperLedgeHits[left].xCoordinate > playerX) {
                    upperLedgeHits[left].xCoordinate + LEDGE_OFFSET
                } else {
                    upperLedgeHits[left].xCoordinate - LEDGE_OFFSET
                }
            }
            if (right < upperLedgeHits.size && !upperLedgeHits[right].hit && lowerLedgeHits[right].hit) {
                return if (upperLedgeHits[right].xCoordinate > playerX) {
                    upperLedgeHits[right].xCoordinate + LEDGE_OFFSET
                } else {
                    upperLedgeHits[right].xCoordinate - LEDGE_OFFSET
                }
            }
        }

        return null
    }

    // get the sensor nearest to the player and search to both sides until a lowerLedgeSensor doesn't hit.
    fun findLedgeToDropDown(
        lowerLedgeHits: GdxArray<LedgeHitData>,
        playerX: Float,
    ): Float? {
        val playerIndex =
            lowerLedgeHits
                .minByOrNull { hit ->
                    abs(hit.xCoordinate - playerX)
                }?.let { hit -> lowerLedgeHits.indexOf(hit) } ?: return null

        for (offset in 0 until lowerLedgeHits.size) {
            val left = playerIndex - offset
            val right = playerIndex + offset

            if (left >= 0 && !lowerLedgeHits[left].hit) {
                return if (lowerLedgeHits[left].xCoordinate > playerX) {
                    lowerLedgeHits[left].xCoordinate + LEDGE_OFFSET
                } else {
                    lowerLedgeHits[left].xCoordinate - LEDGE_OFFSET
                }
            }
            if (right < lowerLedgeHits.size && !lowerLedgeHits[right].hit) {
                return if (lowerLedgeHits[right].xCoordinate > playerX) {
                    lowerLedgeHits[right].xCoordinate + LEDGE_OFFSET
                } else {
                    lowerLedgeHits[right].xCoordinate - LEDGE_OFFSET
                }
            }
        }

        return null
    }

    fun playerIsInChaseRange(): Boolean {
        val selfPos = phyCmp.body.position

        // draw the chase range
        TMP_CIRC
            .set(
                phyCmp.body.position.x,
                phyCmp.body.position.y,
                basicSensorsCmp.chaseRange,
            )
        TMP_CIRC.addToDebugView(debugRenderService, Color.GREEN, "chaseRange")

        // calculate the distance to the player and return true if it is < chaseRange
        val player = world.family { all(PlayerComponent, PhysicComponent) }.firstOrNull() ?: return false
        val playerPos = with(world) { player[PhysicComponent].body.position }
        val dist2 = selfPos.dst2(playerPos)

        return dist2 <= basicSensorsCmp.chaseRange * basicSensorsCmp.chaseRange
    }

    // calculate if the player is above, below or on same platform as enemy
    private fun heightRelationToPlayer(
        self: PhysicComponent,
        player: PhysicComponent,
    ): PlatformRelation {
        val selfBottomY = self.body.position.y + self.offset.y - self.size.y * 0.5f
        val playerBottomY = player.body.position.y + player.offset.y - player.size.y * 0.5f
        val noJumping = player.body.linearVelocity.y == 0f && self.body.linearVelocity.y == 0f
        val dy = selfBottomY - playerBottomY
        return when {
            dy > Y_THRESHOLD && with(world) { playerEntity has HasGroundContact } -> PlatformRelation.ABOVE
            dy < -Y_THRESHOLD -> PlatformRelation.BELOW
            else -> PlatformRelation.SAME
        }
    }

    fun jumpOverGap() {
        if (!rayHitCmp.groundHit && rayHitCmp.jumpHit) {
            intentionCmp.wantsToJump = true
        }
    }

    fun jumpOverWall() {
        if (rayHitCmp.wallHit && !rayHitCmp.wallHeightHit) {
            intentionCmp.wantsToJump = true
        }
    }

    companion object {
        val logger = logger<MushroomContext>()
        val TMP_RECT = Rectangle()
        val TMP_CIRC = Circle()
    }
}

enum class PlatformRelation { SAME, ABOVE, BELOW }
