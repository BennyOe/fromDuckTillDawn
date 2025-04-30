package unitTests

import com.badlogic.gdx.math.Vector2
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import com.github.quillraven.fleks.configureWorld
import io.bennyoe.GameConstants.PHYSIC_TIME_STEP
import io.bennyoe.components.JumpComponent
import io.bennyoe.systems.JumpSystem
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.math.sqrt
import kotlin.test.assertEquals
import com.badlogic.gdx.physics.box2d.World as Box2DWorld

class JumpSystemUnitTest {
    private lateinit var ecsWorld: World
    private lateinit var entity: Entity
    private lateinit var phyWorld: Box2DWorld

    @BeforeEach
    fun setup() {
        phyWorld = Box2DWorld(Vector2(0f, -9.81f), true)

        ecsWorld =
            configureWorld {
                systems { add(JumpSystem(phyWorld)) }
            }

        entity =
            ecsWorld.entity {
                it += JumpComponent(maxHeight = 3f)
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
        with(ecsWorld) { entity[JumpComponent].maxHeight = 0f }

        ecsWorld.update(0f)

        val jump = with(ecsWorld) { entity[JumpComponent] }
        assertEquals(0f, jump.jumpVelocity, "Velocity should be 0 when height ≤ 0")
    }

    @Test
    fun `computed velocity matches analytical formula`() {
        val desiredHeight = 3f

        ecsWorld.update(0f)

        val jump = with(ecsWorld) { entity[JumpComponent] }
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
        ecsWorld.update(0f)
        val normalVel = with(ecsWorld) { entity[JumpComponent].jumpVelocity }

        // make gravity weaker (half)
        phyWorld.gravity = Vector2(0f, -4.905f)
        ecsWorld.update(0f)
        val weakerVel = with(ecsWorld) { entity[JumpComponent].jumpVelocity }

        assert(weakerVel < normalVel) {
            "With weaker gravity the jump velocity should decrease"
        }
    }
}
