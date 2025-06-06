package integrationTests

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ai.fsm.DefaultStateMachine
import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.physics.box2d.Body
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import com.github.quillraven.fleks.configureWorld
import io.bennyoe.components.AnimationComponent
import io.bennyoe.components.AttackComponent
import io.bennyoe.components.HealthComponent
import io.bennyoe.components.ImageComponent
import io.bennyoe.components.InputComponent
import io.bennyoe.components.IntentionComponent
import io.bennyoe.components.JumpComponent
import io.bennyoe.components.MoveComponent
import io.bennyoe.components.PhysicComponent
import io.bennyoe.components.StateComponent
import io.bennyoe.state.player.PlayerCheckAliveState
import io.bennyoe.state.player.PlayerFSM
import io.bennyoe.state.player.PlayerStateContext
import io.bennyoe.systems.InputSystem
import io.bennyoe.systems.MoveSystem
import io.bennyoe.systems.StateSystem
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class MovementIntegrationTest {
    private lateinit var world: World
    private lateinit var entity: Entity

    @BeforeEach
    fun setup() {
        Gdx.app = mockk<Application>(relaxed = true)
        val animationMock = mockk<Animation<TextureRegionDrawable>>(relaxed = true)
        val bodyMock = mockk<Body>(relaxed = true)
        val stageMock = mockk<Stage>(relaxed = true)

        val imageMock: Image = mockk(relaxed = true)
        val imgCmp =
            ImageComponent(stageMock).also {
                it.image = imageMock
            }

        world =
            configureWorld {
                systems {
                    add(InputSystem())
                    add(MoveSystem())
                    add(StateSystem())
                }
            }

        entity =
            world.entity {
                it += AttackComponent()
                it += PhysicComponent().apply { body = bodyMock }
                it += MoveComponent(maxSpeed = 10f)
                it += HealthComponent()
                it += IntentionComponent()
                it += InputComponent()
                it += JumpComponent()
                it += AnimationComponent().apply { animation = animationMock }
                it += imgCmp
                it +=
                    StateComponent(
                        world,
                        PlayerStateContext(it, world),
                        PlayerFSM.IDLE,
                        PlayerCheckAliveState,
                        ::DefaultStateMachine,
                    )
            }
    }

    @Test
    fun `input RIGHT leads to WALK state and maximum velocity`() {
        val inputCmp = with(world) { entity[InputComponent] }
        val stateCmp = with(world) { entity[StateComponent] }
        val moveCmp = with(world) { entity[MoveComponent] }

        inputCmp.walkRightJustPressed = true
        repeat(10) { world.update(0.016f) }

        assertEquals(PlayerFSM.WALK, stateCmp.stateMachine.currentState)
        assertEquals(10f, moveCmp.moveVelocity)
    }

    @Test
    fun `releasing direction returns to IDLE state and zero velocity`() {
        val inputCmp = with(world) { entity[InputComponent] }
        val stateCmp = with(world) { entity[StateComponent] }
        val moveCmp = with(world) { entity[MoveComponent] }

        inputCmp.walkRightJustPressed = true
        world.update(0.016f)
        assertEquals(PlayerFSM.WALK, stateCmp.stateMachine.currentState)

        inputCmp.walkRightJustPressed = false
        world.update(0.016f)

        assertEquals(PlayerFSM.IDLE, stateCmp.stateMachine.currentState)
        assertEquals(0f, moveCmp.moveVelocity)
    }

    @Test
    fun `input RIGHT does nothing when in DEATH state`() {
        val inputCmp = with(world) { entity[InputComponent] }
        val moveCmp = with(world) { entity[MoveComponent] }

        @Suppress("UNCHECKED_CAST")
        val stateCmp: StateComponent<PlayerStateContext, PlayerFSM> =
            with(world) { entity[StateComponent] as StateComponent<PlayerStateContext, PlayerFSM> }

        stateCmp.changeState(PlayerFSM.DEATH)

        inputCmp.walkRightJustPressed = true
        repeat(10) { world.update(0.016f) }

        assertEquals(PlayerFSM.DEATH, stateCmp.stateMachine.currentState)
        assertEquals(0f, moveCmp.moveVelocity)
    }
}
