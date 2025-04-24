import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.Body
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import com.github.quillraven.fleks.configureWorld
import io.bennyoe.ai.PlayerFSM
import io.bennyoe.ai.StateContext
import io.bennyoe.components.AiComponent
import io.bennyoe.components.AnimationComponent
import io.bennyoe.components.InputComponent
import io.bennyoe.components.MoveComponent
import io.bennyoe.components.PhysicComponent
import io.bennyoe.components.WalkDirection
import io.bennyoe.systems.AiSystem
import io.bennyoe.systems.MoveSystem
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class PlayerFSMTest {
    private lateinit var world: World
    private lateinit var entity: Entity
    private lateinit var stateContext: StateContext
    private lateinit var bodyMock: Body

    @BeforeEach
    fun setup() {
        val mockApp = mockk<Application>(relaxed = true)
        Gdx.app = mockApp

        bodyMock = mockk<Body>(relaxed = true)

        val mockAnimationComponent = mockk<AnimationComponent>(relaxed = true)

        world = configureWorld {
            systems {
                add(MoveSystem())
                add(AiSystem())
            }
        }

        entity = world.entity {
            val physicCmp = PhysicComponent()
            physicCmp.body = bodyMock
            it += physicCmp
            it += MoveComponent(maxSpeed = 10f)
            it += InputComponent()
            it += mockAnimationComponent
            it += AiComponent(world)
        }
        stateContext = StateContext(entity, world)
    }


    /*
    States:
    Idle
    Walk
    Jump
    Double Jump
    Fall
    Crouch Idle
    Crouch Walk
    Bash
    Attack 1
    Attack 2
    Attack 3

    Positive transitions:
    Idle -> Jump
    Idle -> Walk
    Idle -> Crouch Idle
    Idle -> Crouch Walk
    Idle -> Bash
    Idle -> Attack 1

    Walk -> Idle
    Walk -> Jump
    Walk -> Crouch Idle
    Walk -> Crouch Walk
    Walk -> Fall
    Walk -> Bash
    Walk -> Attack 1

    Jump -> Double Jump
    Jump -> Fall
    Jump -> Bash
    Jump -> Attack 1

    Double Jump -> Fall
    Double Jump -> Bash
    Double Jump -> Attack 1

    Fall -> Double Jump
    Fall -> Idle
    Fall -> Bash
    Fall -> Attack 1

    Crouch Idle -> Crouch Walk
    Crouch Idle -> Idle
    Crouch Idle -> Walk

    Crouch Walk -> Crouch Idle
    Crouch Walk -> Idle
    Crouch Walk -> Walk

    Bash -> Idle
    Bash-> Jump

    Attack 1 -> Idle
    Attack 1 -> Attack 2
    Attack 1 -> Fall

    Attack 2 -> Idle
    Attack 2 -> Attack 3
    Attack 2 -> Fall

    Attack 3 -> Idle
    Attack 3 -> Fall

    Negative transitions:
    Idle -> Double Jump
    Idle -> Attack 2
    Idle -> Attack 3

    Walk -> Double Jump
    Walk -> Attack 2
    Walk -> Attack 3

    Jump -> Crouch Idle
    Jump -> Crouch Walk
    Jump -> Attack 2
    Jump -> Attack 3
    Jump -> Idle
    Jump -> Walk


    Double Jump -> Crouch Idle
    Double Jump -> Crouch Walk
    Double Jump -> Attack 2
    Double Jump -> Attack 3
    Double Jump -> Idle
    Double Jump -> Walk
    Double Jump -> Jump


     */


    @Test
    fun `default state should be IDLE`() {
        val aiComponent = with(world) { entity[AiComponent] }
        assertEquals(PlayerFSM.IDLE, aiComponent.stateMachine.currentState)
    }

    @Test
    fun `when WalkDirection is not NONE then state should be WALK`() {
        val aiComponent = with(world) { entity[AiComponent] }
        val inputComponent = with(world) { entity[InputComponent] }
        inputComponent.direction = WalkDirection.LEFT
        aiComponent.stateMachine.update()
        assertEquals(PlayerFSM.WALK, aiComponent.stateMachine.currentState)

        inputComponent.direction = WalkDirection.NONE
        aiComponent.stateMachine.update()
        assertEquals(PlayerFSM.IDLE, aiComponent.stateMachine.currentState)

        inputComponent.direction = WalkDirection.RIGHT
        aiComponent.stateMachine.update()
        assertEquals(PlayerFSM.WALK, aiComponent.stateMachine.currentState)
    }

    @Test
    fun `when jump is pressed state should be JUMP`() {
        val aiComponent = with(world) { entity[AiComponent] }
        val inputComponent = with(world) { entity[InputComponent] }
        inputComponent.jumpJustPressed = true
        aiComponent.stateMachine.update()
        assertEquals(PlayerFSM.JUMP, aiComponent.stateMachine.currentState)
    }

    @Test
    fun `when jump is double pressed state should be DOUBLE_JUMP`() {
        val aiComponent = with(world) { entity[AiComponent] }
        val inputComponent = with(world) { entity[InputComponent] }
        inputComponent.jumpJustPressed = true
        aiComponent.stateMachine.update()
        inputComponent.jumpJustPressed = true
        aiComponent.stateMachine.update()
        assertEquals(PlayerFSM.DOUBLE_JUMP, aiComponent.stateMachine.currentState)
    }


    @Test
    fun `when bash is pressed from IDLE then state should be BASH`() {
        val aiComponent = with(world) { entity[AiComponent] }
        val inputComponent = with(world) { entity[InputComponent] }

        inputComponent.bashJustPressed = true
        aiComponent.stateMachine.update()
        assertEquals(PlayerFSM.BASH, aiComponent.stateMachine.currentState)
    }

    @Test
    fun `when bash is pressed from JUMP then state should be BASH`() {
        val aiComponent = with(world) { entity[AiComponent] }
        val inputComponent = with(world) { entity[InputComponent] }

        inputComponent.jumpJustPressed = true
        aiComponent.stateMachine.update()
        assertEquals(PlayerFSM.JUMP, aiComponent.stateMachine.currentState)

        inputComponent.bashJustPressed = true
        aiComponent.stateMachine.update()
        assertEquals(PlayerFSM.BASH, aiComponent.stateMachine.currentState)
    }

    @Test
    fun `should not allow transition from IDLE to ATTACK_2`() {
        val aiComponent = with(world) { entity[AiComponent] }
        val inputComponent = with(world) { entity[InputComponent] }

        aiComponent.stateMachine.changeState(PlayerFSM.IDLE)
        inputComponent.attackJustPressed = true
        aiComponent.stateMachine.update()
        aiComponent.stateMachine.update()

        assertEquals(PlayerFSM.ATTACK_1, aiComponent.stateMachine.currentState)
    }

    @Test
    fun `should not allow transition from DOUBLE_JUMP to JUMP`() {
        val aiComponent = with(world) { entity[AiComponent] }
        val inputComponent = with(world) { entity[InputComponent] }

        inputComponent.jumpJustPressed = true
        aiComponent.stateMachine.update()
        inputComponent.jumpJustPressed = true
        aiComponent.stateMachine.update()
        assertEquals(PlayerFSM.DOUBLE_JUMP, aiComponent.stateMachine.currentState)

        inputComponent.jumpJustPressed = true
        aiComponent.stateMachine.update()
        assertEquals(PlayerFSM.DOUBLE_JUMP, aiComponent.stateMachine.currentState)
    }


    @Test
    fun `when crouch is pressed then state should be CROUCH_IDLE`() {
        val aiComponent = with(world) { entity[AiComponent] }
        val inputComponent = with(world) { entity[InputComponent] }
        inputComponent.crouch = true
        aiComponent.stateMachine.update()
        assertEquals(PlayerFSM.CROUCH_IDLE, aiComponent.stateMachine.currentState)
    }

    @Test
    fun `when crouch is pressed when walking then state should be CROUCH_WALK`() {
        val aiComponent = with(world) { entity[AiComponent] }
        val inputComponent = with(world) { entity[InputComponent] }
        inputComponent.direction = WalkDirection.LEFT
        aiComponent.stateMachine.update()
        inputComponent.crouch = true
        aiComponent.stateMachine.update()
        assertEquals(PlayerFSM.CROUCH_WALK, aiComponent.stateMachine.currentState)
    }

    @Test
    fun `when jump is pressed and then state should be FALLING`() {
        val aiComponent = with(world) { entity[AiComponent] }
        val inputComponent = with(world) { entity[InputComponent] }
        inputComponent.jumpJustPressed = true
        aiComponent.stateMachine.update()
        assertEquals(PlayerFSM.JUMP, aiComponent.stateMachine.currentState)

        givenNegativeYVelocity()
        aiComponent.stateMachine.update()
        assertEquals(PlayerFSM.FALL, aiComponent.stateMachine.currentState)
    }

    @Test
    fun `crouch when jumping should not be possible`() {
        val aiComponent = with(world) { entity[AiComponent] }
        val inputComponent = with(world) { entity[InputComponent] }
        inputComponent.jumpJustPressed = true
        aiComponent.stateMachine.update()
        assertEquals(PlayerFSM.JUMP, aiComponent.stateMachine.currentState)
        inputComponent.crouch = true
        aiComponent.stateMachine.update()
        assertEquals(PlayerFSM.JUMP, aiComponent.stateMachine.currentState)
    }

    @Test
    fun `jump when crouching should not be possible`() {
        val aiComponent = with(world) { entity[AiComponent] }
        val inputComponent = with(world) { entity[InputComponent] }
        inputComponent.crouch = true
        aiComponent.stateMachine.update()
        assertEquals(PlayerFSM.CROUCH_IDLE, aiComponent.stateMachine.currentState)
        inputComponent.jumpJustPressed = true
        aiComponent.stateMachine.update()
        assertEquals(PlayerFSM.CROUCH_IDLE, aiComponent.stateMachine.currentState)
    }

    @Test
    fun `attack sequence from ATTACK_1 to ATTACK_3 then IDLE`() {
        val aiComponent = with(world) { entity[AiComponent] }
        val inputComponent = with(world) { entity[InputComponent] }
        val animationComponent = with(world) { entity[AnimationComponent] }
        every { animationComponent.animation.isAnimationFinished(any()) } returns true

        inputComponent.attackJustPressed = true
        aiComponent.stateMachine.update()
        assertEquals(PlayerFSM.ATTACK_1, aiComponent.stateMachine.currentState)

        inputComponent.attackJustPressed = true
        aiComponent.stateMachine.update()
        assertEquals(PlayerFSM.ATTACK_2, aiComponent.stateMachine.currentState)

        inputComponent.attackJustPressed = true
        aiComponent.stateMachine.update()
        assertEquals(PlayerFSM.ATTACK_3, aiComponent.stateMachine.currentState)

        inputComponent.attackJustPressed = false
        aiComponent.stateMachine.update()
        assertEquals(PlayerFSM.IDLE, aiComponent.stateMachine.currentState)
    }

    @Test
    fun `should not allow ATTACK_2 from JUMP`() {
        val aiComponent = with(world) { entity[AiComponent] }
        val inputComponent = with(world) { entity[InputComponent] }

        inputComponent.jumpJustPressed = true
        aiComponent.stateMachine.update()
        assertEquals(PlayerFSM.JUMP, aiComponent.stateMachine.currentState)

        inputComponent.attackJustPressed = true
        aiComponent.stateMachine.update()
        aiComponent.stateMachine.update()
        assertNotEquals(PlayerFSM.ATTACK_2, aiComponent.stateMachine.currentState)
    }

    private fun givenNegativeYVelocity() {
        val velocity = Vector2(0f, -5f)
        every { bodyMock.linearVelocity } returns velocity
    }

    @Test
    fun `should transition from FALL to IDLE when landing`() {
        val aiComponent = with(world) { entity[AiComponent] }

        // Set initial state to FALL
        aiComponent.stateMachine.changeState(PlayerFSM.FALL)

        // Simulate landing (no vertical velocity)
        val velocity = Vector2(0f, 0f)
        every { bodyMock.linearVelocity } returns velocity

        aiComponent.stateMachine.update()
        assertEquals(PlayerFSM.IDLE, aiComponent.stateMachine.currentState)
    }

    @Test
    fun `should transition from FALL to DOUBLE_JUMP when jump pressed`() {
        val aiComponent = with(world) { entity[AiComponent] }
        val inputComponent = with(world) { entity[InputComponent] }

        // Set initial state to FALL
        aiComponent.stateMachine.changeState(PlayerFSM.FALL)

        inputComponent.jumpJustPressed = true
        aiComponent.stateMachine.update()
        assertEquals(PlayerFSM.DOUBLE_JUMP, aiComponent.stateMachine.currentState)
    }

    @Test
    fun `should transition from BASH to IDLE when animation finished`() {
        val aiComponent = with(world) { entity[AiComponent] }
        val animationComponent = with(world) { entity[AnimationComponent] }

        // Set initial state to BASH
        aiComponent.stateMachine.changeState(PlayerFSM.BASH)

        // Simulate animation finished
        every { animationComponent.animation.isAnimationFinished(any()) } returns true

        aiComponent.stateMachine.update()
        assertEquals(PlayerFSM.IDLE, aiComponent.stateMachine.currentState)
    }

    @Test
    fun `should transition from BASH to JUMP when animation finished and jump pressed`() {
        val aiComponent = with(world) { entity[AiComponent] }
        val inputComponent = with(world) { entity[InputComponent] }
        val animationComponent = with(world) { entity[AnimationComponent] }

        // Set initial state to BASH
        aiComponent.stateMachine.changeState(PlayerFSM.BASH)

        // Simulate animation finished and jump pressed
        every { animationComponent.animation.isAnimationFinished(any()) } returns true
        inputComponent.jumpJustPressed = true

        aiComponent.stateMachine.update()
        assertEquals(PlayerFSM.JUMP, aiComponent.stateMachine.currentState)
    }

    @Test
    fun `should transition from CROUCH_IDLE to IDLE when crouch released`() {
        val aiComponent = with(world) { entity[AiComponent] }
        val inputComponent = with(world) { entity[InputComponent] }

        // Set initial state to CROUCH_IDLE
        inputComponent.crouch = true
        aiComponent.stateMachine.update()
        assertEquals(PlayerFSM.CROUCH_IDLE, aiComponent.stateMachine.currentState)

        // Release crouch
        inputComponent.crouch = false
        aiComponent.stateMachine.update()
        assertEquals(PlayerFSM.IDLE, aiComponent.stateMachine.currentState)
    }

    @Test
    fun `should transition from CROUCH_WALK to CROUCH_IDLE when direction is NONE`() {
        val aiComponent = with(world) { entity[AiComponent] }
        val inputComponent = with(world) { entity[InputComponent] }

        // Set up CROUCH_WALK state
        inputComponent.crouch = true
        inputComponent.direction = WalkDirection.LEFT
        aiComponent.stateMachine.update()
        assertEquals(PlayerFSM.CROUCH_WALK, aiComponent.stateMachine.currentState)

        // Stop walking but keep crouching
        inputComponent.direction = WalkDirection.NONE
        aiComponent.stateMachine.update()
        assertEquals(PlayerFSM.CROUCH_IDLE, aiComponent.stateMachine.currentState)
    }

    @Test
    fun `should transition from WALK to FALL when falling`() {
        val aiComponent = with(world) { entity[AiComponent] }
        val inputComponent = with(world) { entity[InputComponent] }

        // Set up WALK state
        inputComponent.direction = WalkDirection.RIGHT
        aiComponent.stateMachine.update()
        assertEquals(PlayerFSM.WALK, aiComponent.stateMachine.currentState)

        // Simulate falling
        givenNegativeYVelocity()
        aiComponent.stateMachine.update()
        assertEquals(PlayerFSM.FALL, aiComponent.stateMachine.currentState)
    }

    @Test
    fun `should transition from ATTACK_1 to FALL when falling`() {
        val aiComponent = with(world) { entity[AiComponent] }
        val inputComponent = with(world) { entity[InputComponent] }

        // Set up ATTACK_1 state
        inputComponent.attackJustPressed = true
        aiComponent.stateMachine.update()
        assertEquals(PlayerFSM.ATTACK_1, aiComponent.stateMachine.currentState)

        // Simulate falling
        givenNegativeYVelocity()
        aiComponent.stateMachine.update()
        assertEquals(PlayerFSM.FALL, aiComponent.stateMachine.currentState)
    }

    @Test
    fun `should transition from ATTACK_2 to FALL when falling`() {
        val aiComponent = with(world) { entity[AiComponent] }
        val inputComponent = with(world) { entity[InputComponent] }
        val animationComponent = with(world) { entity[AnimationComponent] }

        // Set up ATTACK_1 state first
        inputComponent.attackJustPressed = true
        aiComponent.stateMachine.update()

        // Setup for ATTACK_2
        every { animationComponent.animation.isAnimationFinished(any()) } returns true
        inputComponent.attackJustPressed = true
        aiComponent.stateMachine.update()
        assertEquals(PlayerFSM.ATTACK_2, aiComponent.stateMachine.currentState)

        // Simulate falling
        givenNegativeYVelocity()
        aiComponent.stateMachine.update()
        assertEquals(PlayerFSM.FALL, aiComponent.stateMachine.currentState)
    }

    @Test
    fun `should transition from DOUBLE_JUMP to BASH when bash pressed`() {
        val aiComponent = with(world) { entity[AiComponent] }
        val inputComponent = with(world) { entity[InputComponent] }

        // Setup DOUBLE_JUMP state
        inputComponent.jumpJustPressed = true
        aiComponent.stateMachine.update()
        inputComponent.jumpJustPressed = true
        aiComponent.stateMachine.update()
        assertEquals(PlayerFSM.DOUBLE_JUMP, aiComponent.stateMachine.currentState)

        // Press bash
        inputComponent.bashJustPressed = true
        aiComponent.stateMachine.update()
        assertEquals(PlayerFSM.BASH, aiComponent.stateMachine.currentState)
    }

    @Test
    fun `should transition from ATTACK_3 to IDLE when animation finished`() {
        val aiComponent = with(world) { entity[AiComponent] }
        val animationComponent = with(world) { entity[AnimationComponent] }

        // Setup ATTACK_3 state directly
        aiComponent.stateMachine.changeState(PlayerFSM.ATTACK_3)

        // Simulate animation finished
        every { animationComponent.animation.isAnimationFinished(any()) } returns true

        aiComponent.stateMachine.update()
        assertEquals(PlayerFSM.IDLE, aiComponent.stateMachine.currentState)
    }

    @Test
    fun `should not allow transition from JUMP to IDLE`() {
        val aiComponent = with(world) { entity[AiComponent] }

        // Set initial state to JUMP
        aiComponent.stateMachine.changeState(PlayerFSM.JUMP)

        // Try to change to IDLE
        aiComponent.stateMachine.changeState(PlayerFSM.IDLE)

        // Should still be in JUMP state
        assertEquals(PlayerFSM.JUMP, aiComponent.stateMachine.currentState)
    }

    @Test
    fun `should not allow transition from DOUBLE_JUMP to CROUCH_WALK`() {
        val aiComponent = with(world) { entity[AiComponent] }
        val inputComponent = with(world) { entity[InputComponent] }

        // Set up DOUBLE_JUMP state
        aiComponent.stateMachine.changeState(PlayerFSM.DOUBLE_JUMP)

        // Try to crouch and walk
        inputComponent.crouch = true
        inputComponent.direction = WalkDirection.RIGHT
        aiComponent.stateMachine.update()

        // Should still be in DOUBLE_JUMP state
        assertEquals(PlayerFSM.DOUBLE_JUMP, aiComponent.stateMachine.currentState)
    }
}
