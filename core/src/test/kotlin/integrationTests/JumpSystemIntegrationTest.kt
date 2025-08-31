package integrationTests

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.Body
import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import com.github.quillraven.fleks.configureWorld
import io.bennyoe.components.AnimationComponent
import io.bennyoe.components.AttackComponent
import io.bennyoe.components.FlashlightComponent
import io.bennyoe.components.HasGroundContact
import io.bennyoe.components.HealthComponent
import io.bennyoe.components.InputComponent
import io.bennyoe.components.IntentionComponent
import io.bennyoe.components.JumpComponent
import io.bennyoe.components.MoveComponent
import io.bennyoe.components.PhysicComponent
import io.bennyoe.components.StateComponent
import io.bennyoe.config.GameConstants.DOUBLE_JUMP_GRACE_TIME
import io.bennyoe.config.GameConstants.JUMP_MAX_HEIGHT
import io.bennyoe.lightEngine.core.GameLight
import io.bennyoe.state.player.PlayerCheckAliveState
import io.bennyoe.state.player.PlayerFSM
import io.bennyoe.state.player.PlayerStateContext
import io.bennyoe.systems.InputSystem
import io.bennyoe.systems.JumpSystem
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import com.badlogic.gdx.physics.box2d.World as Box2DWorld

class JumpSystemIntegrationTest {
    private lateinit var world: World
    private lateinit var entity: Entity
    private lateinit var phyWorld: Box2DWorld
    private lateinit var mockAnimationCmp: AnimationComponent
    private lateinit var spotLight: GameLight.Spot
    private lateinit var pointLight: GameLight.Point
    private lateinit var mockBody: Body
    private lateinit var stage: Stage

    @BeforeEach
    fun setup() {
        Gdx.app = mockk<Application>(relaxed = true)
        mockAnimationCmp = mockk<AnimationComponent>(relaxed = true)
        spotLight = mockk<GameLight.Spot>(relaxed = true)
        pointLight = mockk<GameLight.Point>(relaxed = true)
        mockBody = mockk<Body>(relaxed = true)
        stage = mockk<Stage>(relaxed = true)

        phyWorld = Box2DWorld(Vector2(0f, -9.81f), true)

        world =
            configureWorld {
                injectables {
                    add("phyWorld", phyWorld)
                }
                systems {
                    add(InputSystem())
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
                it += IntentionComponent()
                it += FlashlightComponent(spotLight, pointLight)
                it += HealthComponent()
                it += InputComponent()
                it += JumpComponent()
                it +=
                    StateComponent(
                        world,
                        PlayerStateContext(it, world, stage),
                        PlayerFSM.IDLE,
                        PlayerCheckAliveState,
                    )
            }
    }

    @Test
    fun `jump velocity is calculated based on maxHeight`() {
        val jumpCmp = with(world) { entity[JumpComponent] }
        jumpCmp.wantsToJump = true

        world.update(0.016f)

        assertTrue(jumpCmp.jumpVelocity > 0f, "Jump velocity should be positive")
    }

    @Test
    fun `jump height affects jump velocity`() {
        val jumpCmp = with(world) { entity[JumpComponent] }
        jumpCmp.wantsToJump = true

        // Create a second entity with a higher jump height
        val entity2 = createNewEntity(JUMP_MAX_HEIGHT + 5, world)
        val jumpCmp2 = with(world) { entity2[JumpComponent] }
        jumpCmp2.wantsToJump = true

        world.update(0.016f)

        assertTrue(
            jumpCmp2.jumpVelocity > jumpCmp.jumpVelocity,
            "Entity with higher maxHeight should have higher jump velocity",
        )
    }

    @Test
    fun `jump velocity scales with gravity`() {
        val jumpCmp = with(world) { entity[JumpComponent] }
        jumpCmp.wantsToJump = true
        // First jump with normal gravity
        world.update(0.016f)
        val normalGravityVelocity = jumpCmp.jumpVelocity

        // Create a new Box2d-world with reduced gravity
        val reducedGravityWorld = Box2DWorld(Vector2(0f, -2.905f), true)

        // Configure a new Entity-world with the reduced gravity
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
        val reducedGravityEntity = createNewEntity(3f, reducedGravityEcsWorld)
        val jumpCmp2 = with(reducedGravityEcsWorld) { reducedGravityEntity[JumpComponent] }
//        jumpCmp2.wantsToJump = true

        // Apply jump with reduced gravity
        reducedGravityEcsWorld.update(0.016f)
        val reducedGravityVelocity = jumpCmp2.jumpVelocity

        // With lower gravity magnitude, we need less velocity to reach the same height
        assertTrue(reducedGravityVelocity != 0f)
        assertTrue(
            reducedGravityVelocity < normalGravityVelocity,
            "Jump velocity should be lower with reduced gravity magnitude",
        )
    }

    @Test
    fun `jump velocity is zero for non-positive height`() {
        val zeroHeightEntity = createNewEntity(0f, world)
        val jumpCmp = with(world) { zeroHeightEntity[JumpComponent] }
        jumpCmp.wantsToJump = true

        world.update(0.016f)

        val jumpVelocity = jumpCmp.jumpVelocity
        assertEquals(0f, jumpVelocity, "Jump velocity should be zero for non-positive height")
    }

    @Test
    fun `double jump should be possible in DOUBLE_JUMP_GRACE_TIME`() {
        val jumpCmp = with(world) { entity[JumpComponent] }

        @Suppress("UNCHECKED_CAST")
        val stateCmp = with(world) { entity[StateComponent] as StateComponent<PlayerStateContext, PlayerFSM> }
        val intentionCmp = with(world) { entity[IntentionComponent] }
        val deltaTime = 0.05f

        stateCmp.changeState(PlayerFSM.JUMP)
        stateCmp.stateMachine.update()
        stateCmp.changeState(PlayerFSM.FALL)
        world.update(deltaTime)
        assertEquals(DOUBLE_JUMP_GRACE_TIME - deltaTime, jumpCmp.doubleJumpGraceTimer)
        intentionCmp.wantsToJump = true
        stateCmp.stateMachine.update()
        assertEquals(PlayerFSM.DOUBLE_JUMP, stateCmp.stateMachine.currentState)
    }

    @Test
    fun `double jump should NOT be possible outside of DOUBLE_JUMP_GRACE_TIME`() {
        val jumpCmp = with(world) { entity[JumpComponent] }

        @Suppress("UNCHECKED_CAST")
        val stateCmp = with(world) { entity[StateComponent] as StateComponent<PlayerStateContext, PlayerFSM> }
        val inputCmp = with(world) { entity[InputComponent] }
        val intentionCmp = with(world) { entity[IntentionComponent] }
        val deltaTime = 0.1f

        stateCmp.changeState(PlayerFSM.FALL)
        world.update(deltaTime)
        assertEquals(PlayerFSM.FALL, stateCmp.stateMachine.currentState)
        world.update(deltaTime)
        assertEquals(PlayerFSM.FALL, stateCmp.stateMachine.currentState)
        world.update(deltaTime)
        assertEquals(PlayerFSM.FALL, stateCmp.stateMachine.currentState)
        assertTrue(jumpCmp.doubleJumpGraceTimer < 0)
        inputCmp.jumpJustPressed = true
        with(world) { entity.configure { it += HasGroundContact } }
        world.update(deltaTime)
        stateCmp.stateMachine.update()
        assertTrue(intentionCmp.wantsToJump)
        assertEquals(PlayerFSM.IDLE, stateCmp.stateMachine.currentState)
    }

    @Test
    fun `jump should be possible in jumpBuffer time`() {
        @Suppress("UNCHECKED_CAST")
        val stateCmp = with(world) { entity[StateComponent] as StateComponent<PlayerStateContext, PlayerFSM> }
        val inputCmp = with(world) { entity[InputComponent] }
        val deltaTime = 0.05f

        stateCmp.changeState(PlayerFSM.FALL)
        inputCmp.jumpJustPressed = true
        world.update(deltaTime)
        stateCmp.stateMachine.update()
        stateCmp.changeState(PlayerFSM.IDLE)
        with(world) { entity.configure { it += HasGroundContact } }
        world.update(deltaTime)
        stateCmp.stateMachine.update()
        assertEquals(PlayerFSM.JUMP, stateCmp.stateMachine.currentState)
    }

    @Test
    fun `jump should NOT be possible outside of jumpBuffer time`() {
        @Suppress("UNCHECKED_CAST")
        val stateCmp = with(world) { entity[StateComponent] as StateComponent<PlayerStateContext, PlayerFSM> }
        val inputCmp = with(world) { entity[InputComponent] }
        val deltaTime = 0.5f

        stateCmp.changeState(PlayerFSM.FALL)
        inputCmp.jumpJustPressed = true
        world.update(deltaTime)
        stateCmp.stateMachine.update()
        with(world) { entity.configure { it += HasGroundContact } }
        world.update(deltaTime)
        stateCmp.stateMachine.update()
        assertNotEquals(PlayerFSM.JUMP, stateCmp.stateMachine.currentState)
        assertEquals(PlayerFSM.IDLE, stateCmp.stateMachine.currentState)
    }

    private fun createNewEntity(
        jumpHeight: Float,
        world: World,
    ): Entity =
        world.entity {
            it += mockAnimationCmp
            val physicCmp = PhysicComponent()
            physicCmp.body = mockBody
            it += physicCmp
            it += MoveComponent()
            it += FlashlightComponent(spotLight, pointLight)
            it += IntentionComponent()
            it += InputComponent()
            it += JumpComponent(maxHeight = jumpHeight)
            it +=
                StateComponent(
                    world,
                    PlayerStateContext(it, world, stage),
                    PlayerFSM.IDLE,
                    PlayerCheckAliveState,
                )
        }
}
