package integrationTests

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.math.Vector2
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import com.github.quillraven.fleks.configureWorld
import io.bennyoe.components.JumpComponent
import io.bennyoe.components.MoveComponent
import io.bennyoe.systems.AiSystem
import io.bennyoe.systems.JumpSystem
import io.bennyoe.systems.MoveSystem
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import com.badlogic.gdx.physics.box2d.World as Box2DWorld

/**
 * Integration tests for the JumpSystem.
 *
 * These tests verify:
 * 1. Jump velocity is correctly calculated based on maxHeight
 * 2. Jump velocity scales with gravity
 * 3. Jump velocity is zero for non-positive heights
 */
class JumpSystemIntegrationTest {
    private lateinit var world: World
    private lateinit var entity: Entity
    private lateinit var phyWorld: Box2DWorld

    @BeforeEach
    fun setup() {
        Gdx.app = mockk<Application>(relaxed = true)

        phyWorld = Box2DWorld(Vector2(0f, -9.81f), true)

        world =
            configureWorld {
                injectables {
                    add("phyWorld", phyWorld)
                }
                systems {
                    add(MoveSystem())
                    add(AiSystem())
                    add(JumpSystem())
                }
            }

        entity =
            world.entity {
                it += JumpComponent(maxHeight = 3f)
                it += MoveComponent()
            }
    }

    @Test
    fun `jump velocity is calculated based on maxHeight`() {
        // Run the system to calculate jump velocity
        world.update(0.016f)

        // Get the jump component and verify its velocity is not zero
        val jumpComponent = with(world) { entity[JumpComponent] }
        assertTrue(jumpComponent.jumpVelocity > 0f, "Jump velocity should be positive")
    }

    @Test
    fun `jump height affects jump velocity`() {
        // Create a second entity with a higher jump height
        val entity2 =
            world.entity {
                it += JumpComponent(maxHeight = 5f) // Higher jump
                it += MoveComponent()
            }

        // Update the world to process both entities
        world.update(0.016f)

        // Get the jump velocities
        val jumpVelocity1 = with(world) { entity[JumpComponent].jumpVelocity }
        val jumpVelocity2 = with(world) { entity2[JumpComponent].jumpVelocity }

        // The entity with higher maxHeight should have a higher jump velocity
        assertTrue(
            jumpVelocity2 > jumpVelocity1,
            "Entity with higher maxHeight should have higher jump velocity",
        )
    }

    @Test
    fun `jump velocity scales with gravity`() {
        // First jump with normal gravity
        world.update(0.016f)
        val normalGravityVelocity = with(world) { entity[JumpComponent].jumpVelocity }

        // Create a new world with reduced gravity
        val reducedGravityWorld = Box2DWorld(Vector2(0f, -4.905f), true)

        // Configure a new world with the reduced gravity
        val reducedGravityEcsWorld =
            configureWorld {
                injectables {
                    add("phyWorld", reducedGravityWorld)
                }
                systems {
                    add(JumpSystem())
                }
            }

        // Create an entity with the same jump height
        val reducedGravityEntity =
            reducedGravityEcsWorld.entity {
                it += JumpComponent(maxHeight = 3f)
            }

        // Apply jump with reduced gravity
        reducedGravityEcsWorld.update(0.016f)
        val reducedGravityVelocity = with(reducedGravityEcsWorld) { reducedGravityEntity[JumpComponent].jumpVelocity }

        // With lower gravity magnitude, we need less velocity to reach the same height
        assertTrue(
            reducedGravityVelocity < normalGravityVelocity,
            "Jump velocity should be lower with reduced gravity magnitude",
        )
    }

    @Test
    fun `jump velocity is zero for non-positive height`() {
        // Create an entity with zero jump height
        val zeroHeightEntity =
            world.entity {
                it += JumpComponent(maxHeight = 0f)
                it += MoveComponent()
            }

        // Update the world
        world.update(0.016f)

        // Verify the jump velocity is zero
        val jumpVelocity = with(world) { zeroHeightEntity[JumpComponent].jumpVelocity }
        assertEquals(0f, jumpVelocity, "Jump velocity should be zero for non-positive height")
    }
}
