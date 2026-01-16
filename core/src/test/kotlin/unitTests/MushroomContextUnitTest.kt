package unitTests

import com.badlogic.gdx.Application
import com.badlogic.gdx.Files
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import com.github.quillraven.fleks.configureWorld
import io.bennyoe.ai.blackboards.MushroomContext
import io.bennyoe.components.AttackComponent
import io.bennyoe.components.HasGroundContact
import io.bennyoe.components.HealthComponent
import io.bennyoe.components.InputComponent
import io.bennyoe.components.IntentionComponent
import io.bennyoe.components.JumpComponent
import io.bennyoe.components.MoveComponent
import io.bennyoe.components.PhysicComponent
import io.bennyoe.components.StateComponent
import io.bennyoe.components.TransformComponent
import io.bennyoe.components.WalkDirection
import io.bennyoe.components.ai.BasicSensorsComponent
import io.bennyoe.components.ai.BasicSensorsHitComponent
import io.bennyoe.components.ai.BehaviorTreeComponent
import io.bennyoe.components.ai.LedgeHitData
import io.bennyoe.components.ai.LedgeSensorsHitComponent
import io.bennyoe.components.ai.NearbyEnemiesComponent
import io.bennyoe.components.ai.SuspicionComponent
import io.bennyoe.components.animation.AnimationComponent
import io.bennyoe.components.characterMarker.PlayerComponent
import io.bennyoe.config.EntityCategory
import io.bennyoe.state.mushroom.MushroomCheckAliveState
import io.bennyoe.state.mushroom.MushroomFSM
import io.bennyoe.state.mushroom.MushroomStateContext
import io.bennyoe.state.player.PlayerCheckAliveState
import io.bennyoe.state.player.PlayerFSM
import io.bennyoe.state.player.PlayerStateContext
import io.bennyoe.systems.debug.NoOpDebugRenderService
import io.bennyoe.utility.EntityBodyData
import io.bennyoe.utility.SensorType
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import ktx.collections.gdxArrayOf
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class MushroomContextUnitTest {
    private lateinit var world: World
    private lateinit var mushroomEntity: Entity
    private lateinit var playerEntity: Entity
    private lateinit var ctx: MushroomContext
    private lateinit var stage: Stage

    private val intentionCmp = IntentionComponent()
    private val healthCmp = HealthComponent()
    private val nearbyCmp = NearbyEnemiesComponent()
    private val transformCmp = mockk<TransformComponent>(relaxed = true)
    private val animCmp = spyk(AnimationComponent())
    private val phyCmp =
        PhysicComponent().apply {
            body =
                mockk(relaxed = true) {
                    every { position } returns Vector2(5f, 0f)
                }
            size.set(1f, 1f)
            offset.set(0f, 0f)
        }
    private val playerPhyCmp =
        PhysicComponent().apply {
            size.set(1f, 1f)
            offset.set(0f, 0f)
        }

    @BeforeEach
    fun setup() {
        Gdx.app = mockk<Application>(relaxed = true)
        Gdx.files = mockk<Files>(relaxed = true)

        val handleMock = mockk<FileHandle>(relaxed = true)

        every { handleMock.readString() } returns "sequence {\n  task {}\n}"

        world = configureWorld { }

        stage = mockk(relaxed = true)

        playerEntity =
            world.entity {
                playerPhyCmp.apply {
                    body =
                        mockk(relaxed = true) {
                            every { position } returns Vector2(4.5f, 0f)
                            every { userData } returns EntityBodyData(it, EntityCategory.PLAYER)
                        }
                }
                it += animCmp
                it += AttackComponent()
                it += playerPhyCmp
                it += IntentionComponent()
                it += MoveComponent()
                it += HealthComponent()
                it += PlayerComponent
                it += InputComponent()
                it += JumpComponent()
                it += HasGroundContact
                it +=
                    StateComponent(
                        world,
                        PlayerStateContext(it, world, stage),
                        PlayerFSM.IDLE,
                        PlayerCheckAliveState,
                    )
            }

        mushroomEntity =
            world.entity {
                it += intentionCmp
                it += healthCmp
                it += nearbyCmp
                it += animCmp
                it += LedgeSensorsHitComponent()
                it += BasicSensorsComponent(emptyList(), 7f, transformCmp, 23f)
                it += BasicSensorsHitComponent()
                it += phyCmp
                it += SuspicionComponent()
                it +=
                    StateComponent(
                        world,
                        MushroomStateContext(it, world, stage),
                        MushroomFSM.IDLE(),
                        MushroomCheckAliveState(),
                    )
            }

        ctx = MushroomContext(mushroomEntity, world, stage, NoOpDebugRenderService())
    }

    @Test
    fun `isAlive delegates to healthCmp`() {
        healthCmp.current = 3f
        assertTrue(ctx.isAlive())

        healthCmp.current = -3f
        assertFalse(ctx.isAlive())
    }

    @Test
    fun `isAnimationFinished delegates to AnimationComponent`() {
        every { animCmp.isAnimationFinished() } returnsMany listOf(false, true)

        assertFalse(ctx.isAnimationFinished())
        assertTrue(ctx.isAnimationFinished())

        verify(exactly = 2) { animCmp.isAnimationFinished() }
    }

    @Test
    fun `canAttack delegates to rayHitComponent`() {
        val rayHitCmp = with(world) { mushroomEntity[BasicSensorsHitComponent] }
        rayHitCmp.setSensorHit(SensorType.ATTACK_SENSOR, true)
        assertTrue(ctx.canAttack())
    }

    @Test
    fun `hasPlayerNearby returns true and sets target when player entity nearby`() {
        nearbyCmp.nearbyEntities += playerEntity

        val result = ctx.hasPlayerNearby()

        assertTrue(result)
        assertEquals(playerEntity, nearbyCmp.target)
    }

    @Test
    fun `hasEnemyNearby returns false when list empty`() {
        nearbyCmp.nearbyEntities.clear()

        val result = ctx.hasPlayerNearby()

        assertFalse(result)
        assertEquals(BehaviorTreeComponent.NO_TARGET, nearbyCmp.target)
    }

    @Test
    fun `playerIsInChaseRange detects player in range`() {
        val playerPhysicCmp = with(world) { playerEntity[PhysicComponent] }
        playerPhysicCmp.body.position.set(13f, 0f)
        assertFalse(ctx.isPlayerInChaseRange())

        playerPhysicCmp.body.position.set(7f, 0f)
        assertTrue(ctx.isPlayerInChaseRange())
    }

    @Test
    fun `stopMovement resets walkDirection to NONE`() {
        intentionCmp.walkDirection = WalkDirection.RIGHT
        ctx.stopMovement()
        assertEquals(WalkDirection.NONE, intentionCmp.walkDirection)
    }

    @Test
    fun `startAttack sets wantsToAttack flag`() {
        ctx.startAttack()
        assertTrue(intentionCmp.wantsToAttack)
    }

    @Test
    fun `stopAttack clears wantsToAttack flag`() {
        intentionCmp.wantsToAttack = true
        ctx.stopAttack()
        assertFalse(intentionCmp.wantsToAttack)
    }

    @Test
    fun `idle stops movement and attack`() {
        intentionCmp.wantsToAttack = true
        intentionCmp.walkDirection = WalkDirection.RIGHT
        ctx.idle()
        assertFalse(intentionCmp.wantsToAttack)
        assertFalse(intentionCmp.wantsToAttack)
    }

    @Test
    fun `patrol reverses the walk direction when hitting wall or gap in ground`() {
        val rayHitCmp = with(world) { mushroomEntity[BasicSensorsHitComponent] }
        intentionCmp.walkDirection = WalkDirection.LEFT
        rayHitCmp.setSensorHit(SensorType.WALL_SENSOR, true)

        ctx.patrol()
        assertEquals(intentionCmp.walkDirection, WalkDirection.RIGHT)

        rayHitCmp.setSensorHit(SensorType.GROUND_DETECT_SENSOR, false)
        ctx.patrol()
        assertEquals(intentionCmp.walkDirection, WalkDirection.LEFT)
    }

    @Test
    fun `chasePlayer resets ledge when on same platform`() {
        val playerPhysicCmp = with(world) { playerEntity[PhysicComponent] }
        playerPhysicCmp.body.position.set(phyCmp.body.position.x, phyCmp.body.position.y)

        ctx.nearestPlatformLedge = 10f
        ctx.nearestPlatformLedgeWithOffset = 10.5f

        ctx.chasePlayer(world)

        assertEquals(null, ctx.nearestPlatformLedge)
        assertEquals(null, ctx.nearestPlatformLedgeWithOffset)
    }

    @Test
    fun `chasePlayer triggers jump when player is below and jump is needed`() {
        val playerPhysicCmp = with(world) { playerEntity[PhysicComponent] }
        val rayHitCmp = with(world) { mushroomEntity[BasicSensorsHitComponent] }
        playerPhysicCmp.body.position.set(phyCmp.body.position.x + 2, phyCmp.body.position.y + 2)

        rayHitCmp.setSensorHit(SensorType.GROUND_DETECT_SENSOR, false)
        rayHitCmp.setSensorHit(SensorType.JUMP_SENSOR, true)

        ctx.chasePlayer(world)

        assertTrue(intentionCmp.wantsToJump)
        assertEquals(WalkDirection.RIGHT, intentionCmp.walkDirection)
    }

    @Test
    fun `chasePlayer finds ledge when player is above`() {
        val playerPhysicCmp = with(world) { playerEntity[PhysicComponent] }
        val ledgeSensorsHitCmp = with(world) { mushroomEntity[LedgeSensorsHitComponent] }
        playerPhysicCmp.body.position.set(phyCmp.body.position.x + 2, phyCmp.body.position.y - 2)

        ledgeSensorsHitCmp.lowerLedgeHits.addAll(
            gdxArrayOf(
                LedgeHitData(true, 2f),
                LedgeHitData(true, 3f),
                LedgeHitData(true, 4f),
                LedgeHitData(false, 4.5f),
                LedgeHitData(false, 5f),
                LedgeHitData(false, 6f),
                LedgeHitData(false, 7f),
            ),
        )

        ctx.chasePlayer(world)

        assertNotNull(ctx.nearestPlatformLedgeWithOffset)
    }

    @Test
    fun `chasePlayer walks towards player without ledge`() {
        val playerPhysicCmp = with(world) { playerEntity[PhysicComponent] }
        playerPhysicCmp.body.position.set(phyCmp.body.position.x + 2, phyCmp.body.position.y)

        ctx.chasePlayer(world)

        assertEquals(WalkDirection.RIGHT, intentionCmp.walkDirection)
    }

    @Test
    fun `mushroom walks towards player when above without jump option`() {
        val playerPhysicCmp = with(world) { playerEntity[PhysicComponent] }
        ctx.nearestPlatformLedgeWithOffset = 3f

        playerPhysicCmp.body.position.set(phyCmp.body.position.x + 3f, phyCmp.body.position.y - 2f)

        ctx.chasePlayer(world)

        assertEquals(WalkDirection.RIGHT, intentionCmp.walkDirection)
        assertFalse(intentionCmp.wantsToJump)
    }

    @Test
    fun `chasePlayer does not find drop ledge when none are valid`() {
        val playerPhysicCmp = with(world) { playerEntity[PhysicComponent] }
        val ledgeSensorsHitCmp = with(world) { mushroomEntity[LedgeSensorsHitComponent] }

        playerPhysicCmp.body.position.set(phyCmp.body.position.x - 2f, phyCmp.body.position.y - 2f)
        ledgeSensorsHitCmp.lowerLedgeHits.addAll(
            gdxArrayOf(
                LedgeHitData(true, 1f),
                LedgeHitData(true, 2f),
                LedgeHitData(true, 3f),
                LedgeHitData(true, 4f),
                LedgeHitData(true, 5f),
                LedgeHitData(true, 6f),
            ),
        )

        ctx.chasePlayer(world)

        assertNull(ctx.nearestPlatformLedge)
        assertEquals(WalkDirection.LEFT, intentionCmp.walkDirection)
    }

    @Test
    fun `chasePlayer jumps when below and ledge is set but no walk direction`() {
        val playerPhysicCmp = with(world) { playerEntity[PhysicComponent] }
        val ledgeSensorsHitCmp = with(world) { mushroomEntity[LedgeSensorsHitComponent] }

        playerPhysicCmp.body.position.set(phyCmp.body.position.x + 2f, phyCmp.body.position.y + 2f)

        ledgeSensorsHitCmp.upperLedgeHits.addAll(
            gdxArrayOf(
                LedgeHitData(true, 2f),
                LedgeHitData(true, 3f),
                LedgeHitData(true, 4f),
                LedgeHitData(false, 4.5f),
                LedgeHitData(false, 5f),
                LedgeHitData(false, 6f),
                LedgeHitData(false, 7f),
            ),
        )
        ledgeSensorsHitCmp.lowerLedgeHits.addAll(
            gdxArrayOf(
                LedgeHitData(true, 2f),
                LedgeHitData(true, 3f),
                LedgeHitData(true, 4f),
                LedgeHitData(true, 4.5f),
                LedgeHitData(true, 5f),
                LedgeHitData(true, 6f),
                LedgeHitData(true, 7f),
            ),
        )
        intentionCmp.walkDirection = WalkDirection.NONE

        ctx.chasePlayer(world)

        assertTrue(intentionCmp.wantsToJump)
    }

    @Test
    fun `chasePlayer jumps over wall when blocked`() {
        val playerPhysicCmp = with(world) { playerEntity[PhysicComponent] }
        val rayHitCmp = with(world) { mushroomEntity[BasicSensorsHitComponent] }

        playerPhysicCmp.body.position.set(phyCmp.body.position.x + 2f, phyCmp.body.position.y)
        rayHitCmp.setSensorHit(SensorType.WALL_SENSOR, true)
        rayHitCmp.setSensorHit(SensorType.WALL_HEIGHT_SENSOR, false)

        ctx.chasePlayer(world)

        assertTrue(intentionCmp.wantsToJump)
    }

    @Test
    fun `chasePlayer does not move or jump when on same platform and player is near`() {
        val playerPhysicCmp = with(world) { playerEntity[PhysicComponent] }
        playerPhysicCmp.body.position.set(phyCmp.body.position.x + 0.05f, phyCmp.body.position.y)

        ctx.nearestPlatformLedge = 8f
        ctx.nearestPlatformLedgeWithOffset = 8.5f

        ctx.chasePlayer(world)

        assertEquals(WalkDirection.NONE, intentionCmp.walkDirection)
        assertFalse(intentionCmp.wantsToJump)
        assertNull(ctx.nearestPlatformLedge)
        assertNull(ctx.nearestPlatformLedgeWithOffset)
    }
}
