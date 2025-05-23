package integrationTests

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.Body
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import com.github.quillraven.fleks.configureWorld
import io.bennyoe.components.AnimationComponent
import io.bennyoe.components.AttackComponent
import io.bennyoe.components.HasGroundContact
import io.bennyoe.components.HealthComponent
import io.bennyoe.components.InputComponent
import io.bennyoe.components.JumpComponent
import io.bennyoe.components.MoveComponent
import io.bennyoe.components.PhysicComponent
import io.bennyoe.components.StateComponent
import io.bennyoe.config.GameConstants.DOUBLE_JUMP_GRACE_TIME
import io.bennyoe.state.PlayerFSM
import io.bennyoe.systems.JumpSystem
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
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
    private lateinit var mockAnimationCmp: AnimationComponent
    private lateinit var mockBody: Body

    @BeforeEach
    fun setup() {
        Gdx.app = mockk<Application>(relaxed = true)
        mockAnimationCmp = mockk<AnimationComponent>(relaxed = true)
        mockBody = mockk<Body>(relaxed = true)

        phyWorld = Box2DWorld(Vector2(0f, -9.81f), true)

        world =
            configureWorld {
                injectables {
                    add("phyWorld", phyWorld)
                }
                systems {
                    add(JumpSystem())
                }
            }

        entity =
            world.entity {
                it += mockAnimationCmp
                val physicCmp = PhysicComponent()
                physicCmp.body = mockBody
                it += physicCmp
                it += AttackComponent()
                it += MoveComponent()
                it += HealthComponent()
                it += InputComponent()
                it += JumpComponent()
                it += StateComponent(world)
            }
    }

    @Test
    fun `jump velocity is calculated based on maxHeight`() {
        val jumpComponent = with(world) { entity[JumpComponent] }
        jumpComponent.wantsToJump = true
        // Run the system to calculate jump velocity
        world.update(0.016f)

        // Get the jump component and verify its velocity is not zero
        assertTrue(jumpComponent.jumpVelocity > 0f, "Jump velocity should be positive")
    }

    @Test
    fun `jump height affects jump velocity`() {
        val jumpComponent = with(world) { entity[JumpComponent] }
        jumpComponent.wantsToJump = true
        // Create a second entity with a higher jump height
        val entity2 =
            world.entity {
                it += mockAnimationCmp
                val physicCmp = PhysicComponent()
                physicCmp.body = mockBody
                it += AttackComponent()
                it += physicCmp
                it += MoveComponent()
                it += InputComponent()
                it += HealthComponent()
                it += JumpComponent(maxHeight = 5f) // Higher jump
                it += StateComponent(world)
            }
        val jumpComponent2 = with(world) { entity2[JumpComponent] }
        jumpComponent2.wantsToJump = true

        // Update the world to process both entities
        world.update(0.016f)

        // The entity with higher maxHeight should have a higher jump velocity
        assertTrue(
            jumpComponent2.jumpVelocity > jumpComponent.jumpVelocity,
            "Entity with higher maxHeight should have higher jump velocity",
        )
    }

    @Test
    fun `jump velocity scales with gravity`() {
        val jumpComponent = with(world) { entity[JumpComponent] }
        jumpComponent.wantsToJump = true
        // First jump with normal gravity
        world.update(0.016f)
        val normalGravityVelocity = with(world) { entity[JumpComponent].jumpVelocity }

        // Create a new world with reduced gravity
        val reducedGravityWorld = Box2DWorld(Vector2(0f, -2.905f), true)

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
                it += mockAnimationCmp
                val physicCmp = PhysicComponent()
                physicCmp.body = mockBody
                it += physicCmp
                it += MoveComponent()
                it += InputComponent()
                it += JumpComponent(maxHeight = 3f)
                it += StateComponent(world)
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
        val jumpComponent = with(world) { entity[JumpComponent] }
        jumpComponent.wantsToJump = true
        // Create an entity with zero jump height
        val zeroHeightEntity =
            world.entity {
                it += mockAnimationCmp
                val physicCmp = PhysicComponent()
                physicCmp.body = mockBody
                it += physicCmp
                it += AttackComponent()
                it += MoveComponent()
                it += HealthComponent()
                it += InputComponent()
                it += JumpComponent(maxHeight = 0f)
                it += StateComponent(world)
            }

        // Update the world
        world.update(0.016f)

        // Verify the jump velocity is zero
        val jumpVelocity = with(world) { zeroHeightEntity[JumpComponent].jumpVelocity }
        assertEquals(0f, jumpVelocity, "Jump velocity should be zero for non-positive height")
    }

    @Test
    fun `double jump should be possible in DOUBLE_JUMP_GRACE_TIME`() {
        val jumpComponent = with(world) { entity[JumpComponent] }
        val stateComponent = with(world) { entity[StateComponent] }
        val inputComponent = with(world) { entity[InputComponent] }
        val dt = 0.05f

        stateComponent.changeState(PlayerFSM.JUMP)
        stateComponent.stateMachine.update()
        stateComponent.changeState(PlayerFSM.FALL)
        world.update(dt)
        assertEquals(DOUBLE_JUMP_GRACE_TIME - dt, jumpComponent.doubleJumpGraceTimer)
        inputComponent.jumpJustPressed = true
        stateComponent.stateMachine.update()
        assertEquals(PlayerFSM.DOUBLE_JUMP, stateComponent.stateMachine.currentState)
    }

    @Test
    fun `double jump should NOT be possible outside of DOUBLE_JUMP_GRACE_TIME`() {
        val jumpComponent = with(world) { entity[JumpComponent] }
        val stateComponent = with(world) { entity[StateComponent] }
        val inputComponent = with(world) { entity[InputComponent] }
        val dt = 0.1f

        stateComponent.changeState(PlayerFSM.FALL)
        world.update(dt)
        assertEquals(PlayerFSM.FALL, stateComponent.stateMachine.currentState)
        world.update(dt)
        assertEquals(PlayerFSM.FALL, stateComponent.stateMachine.currentState)
        world.update(dt)
        assertEquals(PlayerFSM.FALL, stateComponent.stateMachine.currentState)
        assertTrue(jumpComponent.doubleJumpGraceTimer < 0)
        inputComponent.jumpJustPressed = true
        with(world) { entity.configure { it += HasGroundContact } }
        stateComponent.stateMachine.update()
        assertEquals(PlayerFSM.IDLE, stateComponent.stateMachine.currentState)
    }

    @Test
    fun `jump should be possible in jumpBuffer time`() {
        val stateComponent = with(world) { entity[StateComponent] }
        val inputComponent = with(world) { entity[InputComponent] }
        val dt = 0.05f

        stateComponent.changeState(PlayerFSM.FALL)
        inputComponent.jumpJustPressed = true
        world.update(dt)
        stateComponent.stateMachine.update()
        with(world) { entity.configure { it += HasGroundContact } }
        world.update(dt)
        stateComponent.stateMachine.update()
        assertEquals(PlayerFSM.JUMP, stateComponent.stateMachine.currentState)
    }

    @Test
    fun `jump should NOT be possible outside of jumpBuffer time`() {
        val stateComponent = with(world) { entity[StateComponent] }
        val inputComponent = with(world) { entity[InputComponent] }
        val dt = 0.5f

        stateComponent.changeState(PlayerFSM.FALL)
        inputComponent.jumpJustPressed = true
        world.update(dt)
        stateComponent.stateMachine.update()
        with(world) { entity.configure { it += HasGroundContact } }
        world.update(dt)
        stateComponent.stateMachine.update()
        assertNotEquals(PlayerFSM.JUMP, stateComponent.stateMachine.currentState)
        assertEquals(PlayerFSM.IDLE, stateComponent.stateMachine.currentState)
    }
}
