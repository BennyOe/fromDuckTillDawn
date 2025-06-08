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
import io.bennyoe.components.AnimationComponent
import io.bennyoe.components.HealthComponent
import io.bennyoe.components.IntentionComponent
import io.bennyoe.components.PhysicComponent
import io.bennyoe.components.PlayerComponent
import io.bennyoe.components.WalkDirection
import io.bennyoe.components.ai.BehaviorTreeComponent
import io.bennyoe.components.ai.NearbyEnemiesComponent
import io.bennyoe.service.NoOpDebugRenderService
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class MushroomContextUnitTest {
    private lateinit var world: World
    private lateinit var mush: Entity
    private lateinit var ctx: MushroomContext
    private lateinit var stage: Stage

    private val intentionCmp = IntentionComponent()
    private val healthCmp = HealthComponent()
    private val nearbyCmp = NearbyEnemiesComponent()
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

    @BeforeEach
    fun setup() {
        Gdx.app = mockk<Application>(relaxed = true)
        Gdx.files = mockk<Files>(relaxed = true)

        val handleMock = mockk<FileHandle>(relaxed = true)

        every { handleMock.readString() } returns "sequence {\n  task {}\n}"

        world = configureWorld { }

        stage = mockk(relaxed = true)

        mush =
            world.entity {
                it += intentionCmp
                it += healthCmp
                it += nearbyCmp
                it += animCmp
                it += phyCmp
            }

        ctx = MushroomContext(mush, world, stage, NoOpDebugRenderService())
    }

    @Test
    fun `moveTo sets walkDirection RIGHT when target is right of mushroom`() {
        ctx.moveTo(Vector2(10f, 0f))
        assertEquals(WalkDirection.RIGHT, intentionCmp.walkDirection)
    }

    @Test
    fun `moveTo sets walkDirection LEFT when target is left of mushroom`() {
        ctx.moveTo(Vector2(2f, 0f))
        assertEquals(WalkDirection.LEFT, intentionCmp.walkDirection)
    }

    @Test
    fun `stopMovement resets walkDirection to NONE`() {
        intentionCmp.walkDirection = WalkDirection.RIGHT
        ctx.stopMovement()
        assertEquals(WalkDirection.NONE, intentionCmp.walkDirection)
    }

    @Test
    fun `inRange returns true when target inside expanded hitbox`() {
        val inside = Vector2(5.4f, 0.4f)
        assertTrue(ctx.inRange(0.5f, inside))
    }

    @Test
    fun `inRange returns false when target outside expanded hitbox`() {
        val outside = Vector2(2f, 0f)
        assertFalse(ctx.inRange(0.5f, outside))
    }

    @Test
    fun `isAlive delegates to healthCmp`() {
        healthCmp.current = 3f
        assertTrue(ctx.isAlive())

        healthCmp.current = -3f
        assertFalse(ctx.isAlive())
    }

    @Test
    fun `hasEnemyNearby returns true and sets target when player entity nearby`() {
        val player = world.entity { it += PlayerComponent() }
        nearbyCmp.nearbyEntities += player

        val result = ctx.hasEnemyNearby()

        assertTrue(result)
        assertEquals(player, nearbyCmp.target)
    }

    @Test
    fun `hasEnemyNearby returns false when list empty`() {
        nearbyCmp.nearbyEntities.clear()

        val result = ctx.hasEnemyNearby()

        assertFalse(result)
        assertEquals(BehaviorTreeComponent.NO_TARGET, nearbyCmp.target)
    }

    @Test
    fun `isAnimationFinished delegates to AnimationComponent`() {
        every { animCmp.isAnimationFinished() } returnsMany listOf(false, true)

        assertFalse(ctx.isAnimationFinished())
        assertTrue(ctx.isAnimationFinished())

        verify(exactly = 2) { animCmp.isAnimationFinished() }
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
}
