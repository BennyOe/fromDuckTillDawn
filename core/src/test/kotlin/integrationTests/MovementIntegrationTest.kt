package integrationTests

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.physics.box2d.Body
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import com.github.quillraven.fleks.configureWorld
import io.bennyoe.state.PlayerFSM
import io.bennyoe.components.StateComponent
import io.bennyoe.components.AnimationComponent
import io.bennyoe.components.InputComponent
import io.bennyoe.components.JumpComponent
import io.bennyoe.components.MoveComponent
import io.bennyoe.components.PhysicComponent
import io.bennyoe.components.WalkDirection
import io.bennyoe.systems.StateSystem
import io.bennyoe.systems.MoveSystem
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 * Integration-Test: chained **Input → AI → MoveSystem**.
 *
 * 1. Key RIGHT → AI-State = WALK → velocity = maxSpeed
 * 2. Release key → AI-State = IDLE → velocity = 0
 */
class MovementIntegrationTest {
    private lateinit var world: World
    private lateinit var entity: Entity

    @BeforeEach
    fun setup() {
        // Headless-Backend for LibGDX
        Gdx.app = mockk<Application>(relaxed = true)
        val animationMock = mockk<Animation<TextureRegionDrawable>>(relaxed = true)
        val bodyMock = mockk<Body>(relaxed = true)

        world =
            configureWorld {
                systems {
                    add(MoveSystem())
                    add(StateSystem())
                }
            }

        entity =
            world.entity {
                it += PhysicComponent().apply { body = bodyMock }
                it += MoveComponent(maxSpeed = 10f)
                it += InputComponent()
                it += JumpComponent()
                it += AnimationComponent().apply { animation = animationMock }
                it += StateComponent(world)
            }
    }

    @Test
    fun `input RIGHT leads to WALK state and maximum velocity`() {
        val input = with(world) { entity[InputComponent] }
        val ai = with(world) { entity[StateComponent] }
        val move = with(world) { entity[MoveComponent] }

        input.direction = WalkDirection.RIGHT
        repeat(10) { world.update(0.016f) } // ~10 Frames at 60 FPS

        assertEquals(PlayerFSM.WALK, ai.stateMachine.currentState)
        assertEquals(10f, move.moveVelocity)
    }

    @Test
    fun `releasing direction returns to IDLE state and zero velocity`() {
        val input = with(world) { entity[InputComponent] }
        val ai = with(world) { entity[StateComponent] }
        val move = with(world) { entity[MoveComponent] }

        input.direction = WalkDirection.RIGHT
        world.update(0.016f)
        assertEquals(PlayerFSM.WALK, ai.stateMachine.currentState)

        input.direction = WalkDirection.NONE
        world.update(0.016f)

        assertEquals(PlayerFSM.IDLE, ai.stateMachine.currentState)
        assertEquals(0f, move.moveVelocity)
    }
}
