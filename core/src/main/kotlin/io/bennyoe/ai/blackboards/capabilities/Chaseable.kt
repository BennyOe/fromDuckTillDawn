package io.bennyoe.ai.blackboards.capabilities

import com.badlogic.gdx.math.Vector2
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import io.bennyoe.ai.blackboards.PlatformRelation
import io.bennyoe.ai.blackboards.X_THRESHOLD
import io.bennyoe.ai.blackboards.Y_THRESHOLD
import io.bennyoe.components.HasGroundContact
import io.bennyoe.components.IntentionComponent
import io.bennyoe.components.PhysicComponent
import io.bennyoe.components.WalkDirection
import io.bennyoe.components.ai.BasicSensorsComponent
import io.bennyoe.components.ai.BasicSensorsHitComponent
import io.bennyoe.components.ai.LedgeHitData
import io.bennyoe.components.ai.LedgeSensorsHitComponent
import io.bennyoe.utility.SensorType
import ktx.collections.GdxArray
import ktx.collections.isNotEmpty
import ktx.collections.lastIndex
import kotlin.math.abs

interface Chaseable : ChaseState {
    val intentionCmp: IntentionComponent
    val basicSensorsHitCmp: BasicSensorsHitComponent
    val ledgeSensorsHitCmp: LedgeSensorsHitComponent
    val basicSensorsCmp: BasicSensorsComponent
    val playerEntity: Entity
    val phyCmp: PhysicComponent
    val playerPhysicCmp: PhysicComponent

    fun chasePlayer(world: World) {
        val playerPos = with(world) { playerEntity[PhysicComponent].body.position }

        // update state
        if (with(world) { playerEntity has HasGroundContact }) {
            platformRelation = heightRelationToPlayer(phyCmp, playerPhysicCmp)
        }

        // reset if on same platform
        if (platformRelation == PlatformRelation.SAME) {
            nearestPlatformLedge = null
            nearestPlatformLedgeWithOffset = null
        }

        // wall and gap jumps
        if (platformRelation != PlatformRelation.ABOVE) {
            jumpOverWall()
            jumpOverGap()
        }

        changePlatform(playerPos)

        if (platformRelation == PlatformRelation.BELOW) {
            calculateNearestPlatformEdgeOffset(ledgeSensorsHitCmp.upperLedgeHits)
        } else if (platformRelation == PlatformRelation.ABOVE) {
            calculateNearestPlatformEdgeOffset(ledgeSensorsHitCmp.lowerLedgeHits)
        }

        walkToPosition(world)

        // jump when needed
        if (intentionCmp.walkDirection == WalkDirection.NONE &&
            platformRelation == PlatformRelation.BELOW &&
            nearestPlatformLedgeWithOffset != null
        ) {
            intentionCmp.wantsToJump = true
        }
    }

    /**
     * Determines the nearest reachable upper ledge offset for jumping up or dropping down.
     * Starts from the currently selected ledge (`nearestPlatformLedge`) and checks neighboring sensors
     * to the left and right. If a neighboring ledge is not blocked, its x-coordinate is selected
     * as a better jump target.
     */
    private fun calculateNearestPlatformEdgeOffset(hits: GdxArray<LedgeHitData>) {
        val index = hits.indexOfFirst { it.xCoordinate == nearestPlatformLedge }

        if (index == -1 || hits[index].hit) {
            nearestPlatformLedgeWithOffset = null
            return
        }

        if (index > 1 && !hits[index - 2].hit) {
            nearestPlatformLedgeWithOffset = hits[index - 2].xCoordinate
            nearestPlatformLedge = null
        } else if (index < hits.lastIndex - 1 && !hits[index + 2].hit) {
            nearestPlatformLedgeWithOffset = hits[index + 2].xCoordinate
            nearestPlatformLedge = null
        }
    }

    private fun changePlatform(playerPos: Vector2) {
        nearestPlatformLedge =
            when (platformRelation) {
                PlatformRelation.BELOW -> {
                    if (ledgeSensorsHitCmp.upperLedgeHits.size == ledgeSensorsHitCmp.lowerLedgeHits.size &&
                        ledgeSensorsHitCmp.upperLedgeHits.isNotEmpty()
                    ) {
                        findLedgeToJumpUp(ledgeSensorsHitCmp.upperLedgeHits, ledgeSensorsHitCmp.lowerLedgeHits, playerPos.x)
                    } else {
                        null
                    }
                }

                PlatformRelation.ABOVE -> {
                    if (ledgeSensorsHitCmp.lowerLedgeHits.isNotEmpty()) {
                        findLedgeToDropDown(ledgeSensorsHitCmp.lowerLedgeHits, playerPos.x)
                    } else {
                        null
                    }
                }

                else -> {
                    null
                }
            }
    }

    // get the sensor located closest to the player and iterate to both sides until a upperLedgeSensor doesn't hit. If the lowerLedgeSensor hits
    // this is the jump-point to the upper platform
    private fun findLedgeToJumpUp(
        upperLedgeHits: GdxArray<LedgeHitData>,
        lowerLedgeHits: GdxArray<LedgeHitData>,
        playerX: Float,
    ): Float? {
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
                return upperLedgeHits[left].xCoordinate
            }
            if (right < upperLedgeHits.size && !upperLedgeHits[right].hit && lowerLedgeHits[right].hit) {
                return upperLedgeHits[right].xCoordinate
            }
        }

        return null
    }

    /**  same as [findLedgeToJumpUp] for falling down a platform **/
    private fun findLedgeToDropDown(
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

            // maybe the left != playerIndex is not needed
            if (left >= 0 && !lowerLedgeHits[left].hit && left != playerIndex) {
                return lowerLedgeHits[left].xCoordinate
            }
            // maybe the right != playerIndex is not needed
            if (right < lowerLedgeHits.size && !lowerLedgeHits[right].hit && right != playerIndex) {
                return lowerLedgeHits[right].xCoordinate
            }
        }

        return null
    }

    private fun walkToPosition(world: World) {
        val selfPos = phyCmp.body.position
        val playerPos = with(world) { playerEntity[PhysicComponent].body.position }
        val goToPosition: Float = nearestPlatformLedgeWithOffset ?: playerPos.x
        val dist = selfPos.x - goToPosition

        when {
            abs(dist) > X_THRESHOLD -> {
                intentionCmp.walkDirection = if (dist < 0f) WalkDirection.RIGHT else WalkDirection.LEFT
            }

            else -> {
                intentionCmp.walkDirection = WalkDirection.NONE
            }
        }
    }

    private fun jumpOverGap() {
        if (!basicSensorsHitCmp.getSensorHit(SensorType.GROUND_DETECT_SENSOR) && basicSensorsHitCmp.getSensorHit(SensorType.JUMP_SENSOR)) {
            intentionCmp.wantsToJump = true
        }
    }

    private fun jumpOverWall() {
        if (basicSensorsHitCmp.getSensorHit(SensorType.WALL_SENSOR) && !basicSensorsHitCmp.getSensorHit(SensorType.WALL_HEIGHT_SENSOR)) {
            intentionCmp.wantsToJump = true
        }
    }

    // calculate if the player is above, below or on same platform as enemy
    private fun heightRelationToPlayer(
        self: PhysicComponent,
        player: PhysicComponent,
    ): PlatformRelation {
        val selfBottomY = self.body.position.y + self.offset.y - self.size.y * 0.5f
        val playerBottomY = player.body.position.y + player.offset.y - player.size.y * 0.5f
        val dy = selfBottomY - playerBottomY
        return when {
            dy > Y_THRESHOLD -> PlatformRelation.ABOVE
            dy < -Y_THRESHOLD -> PlatformRelation.BELOW
            else -> PlatformRelation.SAME
        }
    }
}

interface ChaseState {
    var nearestPlatformLedge: Float?
    var nearestPlatformLedgeWithOffset: Float?
    var platformRelation: PlatformRelation
}

class DefaultChaseState : ChaseState {
    override var nearestPlatformLedge: Float? = null
    override var nearestPlatformLedgeWithOffset: Float? = null
    override var platformRelation: PlatformRelation = PlatformRelation.SAME
}
