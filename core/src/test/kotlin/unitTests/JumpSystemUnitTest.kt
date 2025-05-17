package unitTests

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.Body
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import com.github.quillraven.fleks.configureWorld
import io.bennyoe.GameConstants.PHYSIC_TIME_STEP
import io.bennyoe.components.AnimationComponent
import io.bennyoe.components.HealthComponent
import io.bennyoe.components.InputComponent
import io.bennyoe.components.JumpComponent
import io.bennyoe.components.MoveComponent
import io.bennyoe.components.PhysicComponent
import io.bennyoe.components.StateComponent
import io.bennyoe.systems.JumpSystem
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.math.sqrt
import kotlin.test.assertEquals
import com.badlogic.gdx.physics.box2d.World as Box2DWorld

class JumpSystemUnitTest {
    private lateinit var world: World
    private lateinit var entity: Entity
    private lateinit var phyWorld: Box2DWorld
    private lateinit var mockBody: Body

    @BeforeEach
    fun setup() {
        val mockAnimationCmp = mockk<AnimationComponent>(relaxed = true)
        phyWorld = Box2DWorld(Vector2(0f, -9.81f), true)
        mockBody = mockk<Body>(relaxed = true)

        world =
            configureWorld {
                injectables {
                    add("phyWorld", phyWorld)
                }
                systems { add(JumpSystem()) }
            }

        entity =
            world.entity {
                it += mockAnimationCmp
                val physicCmp = PhysicComponent()
                physicCmp.body = mockBody
                it += physicCmp
                it += MoveComponent()
                it += HealthComponent()
                it += InputComponent()
                it += JumpComponent()
                it += StateComponent(world)
            }
    }

    /**
     * Helper that re‑implements the formula from JumpSystem for comparison.
     */
    private fun expectedJumpVelocity(height: Float): Float {
        if (height <= 0f) return 0f
        val gravityPerStepY = phyWorld.gravity.y * PHYSIC_TIME_STEP * PHYSIC_TIME_STEP
        val a = 0.5f / gravityPerStepY
        val b = 0.5f
        val disc = b * b - 4 * a * height
        val quad1 = (-b - sqrt(disc)) / (2 * a)
        val quad2 = (-b + sqrt(disc)) / (2 * a)
        val solutionStep = if (quad1 < 0) quad2 else quad1
        return solutionStep / PHYSIC_TIME_STEP
    }

    @Test
    fun `jump velocity is zero for non positive height`() {
        with(world) { entity[JumpComponent].maxHeight = 0f }

        world.update(0f)

        val jump = with(world) { entity[JumpComponent] }
        assertEquals(0f, jump.jumpVelocity, "Velocity should be 0 when height ≤ 0")
    }

    @Test
    fun `computed velocity matches analytical formula`() {
        val desiredHeight = 3f

        world.update(0f)

        val jump = with(world) { entity[JumpComponent] }
        val expected = expectedJumpVelocity(desiredHeight)

        assertEquals(
            expected,
            jump.jumpVelocity,
            1e-3f,
            "Jump velocity should follow the analytical physics formula",
        )
    }

    @Test
    fun `velocity scales with gravity magnitude`() {
        // first run with normal gravity
        world.update(0f)
        val normalVel = with(world) { entity[JumpComponent].jumpVelocity }

        // make gravity weaker (half)
        phyWorld.gravity = Vector2(0f, -4.905f)
        world.update(0f)
        val weakerVel = with(world) { entity[JumpComponent].jumpVelocity }

        assert(weakerVel < normalVel) {
            "With weaker gravity the jump velocity should decrease"
        }
    }
}
