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

    private fun givenNegativeYVelocity() {
        val velocity = Vector2(0f, -5f)
        every { bodyMock.linearVelocity } returns velocity
    }
}
