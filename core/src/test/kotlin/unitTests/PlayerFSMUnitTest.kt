package unitTests

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ai.msg.MessageManager
import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.Body
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import com.github.quillraven.fleks.configureWorld
import io.bennyoe.components.AnimationComponent
import io.bennyoe.components.AttackComponent
import io.bennyoe.components.DeadComponent
import io.bennyoe.components.HasGroundContact
import io.bennyoe.components.HealthComponent
import io.bennyoe.components.ImageComponent
import io.bennyoe.components.InputComponent
import io.bennyoe.components.IntentionComponent
import io.bennyoe.components.JumpComponent
import io.bennyoe.components.MoveComponent
import io.bennyoe.components.PhysicComponent
import io.bennyoe.components.StateComponent
import io.bennyoe.components.WalkDirection
import io.bennyoe.state.FsmMessageTypes
import io.bennyoe.state.player.PlayerCheckAliveState
import io.bennyoe.state.player.PlayerFSM
import io.bennyoe.state.player.PlayerStateContext
import io.bennyoe.systems.MoveSystem
import io.bennyoe.systems.StateSystem
import io.mockk.every
import io.mockk.mockk
import ktx.collections.gdxArrayOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals

class PlayerFSMUnitTest {
    private lateinit var world: World
    private lateinit var entity: Entity
    private lateinit var stateContext: PlayerStateContext
    private lateinit var bodyMock: Body

    @BeforeEach
    fun setup() {
        val mockApp = mockk<Application>(relaxed = true)
        Gdx.app = mockApp

        bodyMock = mockk<Body>(relaxed = true)

        val atlasMock = mockk<TextureAtlas>(relaxed = true)
        val animationMock = mockk<Animation<TextureRegionDrawable>>(relaxed = true)
        val regionMock = mockk<TextureAtlas.AtlasRegion>(relaxed = true)
        val stageMock = mockk<Stage>(relaxed = true)

        val imageMock: Image = mockk(relaxed = true)
        val imgCmp =
            ImageComponent(stageMock).also {
                it.image = imageMock
            }

        every { atlasMock.findRegions(any()) } returns gdxArrayOf(regionMock)
        every { animationMock.isAnimationFinished(any()) } returns false

        val animationCmp =
            AnimationComponent().apply {
                animation = animationMock
            }

        world =
            configureWorld {
                systems {
                    add(MoveSystem())
                    add(StateSystem())
                }
            }

        entity =
            world.entity {
                it += AttackComponent()
                val physicCmp = PhysicComponent()
                physicCmp.body = bodyMock
                it += physicCmp
                it += HealthComponent()
                it += IntentionComponent()
                it += MoveComponent(maxSpeed = 10f)
                it += imgCmp
                it += InputComponent()
                it += animationCmp
                it += JumpComponent()
                it +=
                    DeadComponent(
                        false,
                        0f,
                        0f,
                    )
                it +=
                    StateComponent(
                        world,
                        PlayerStateContext(it, world),
                        PlayerFSM.IDLE,
                        PlayerCheckAliveState,
                    )
            }
        stateContext = PlayerStateContext(entity, world)
    }

    @Test
    fun `default state should be IDLE`() {
        val stateCmp = with(world) { entity[StateComponent] }
        assertEquals(PlayerFSM.IDLE, stateCmp.stateMachine.currentState)
    }

    @Test
    fun `when WalkDirection is not NONE then state should be WALK`() {
        val stateCmp = with(world) { entity[StateComponent] }
        val intentionCmp = with(world) { entity[IntentionComponent] }

        intentionCmp.walkDirection = WalkDirection.LEFT
        stateCmp.stateMachine.update()
        assertEquals(PlayerFSM.WALK, stateCmp.stateMachine.currentState)

        intentionCmp.walkDirection = WalkDirection.NONE
        stateCmp.stateMachine.update()
        assertEquals(PlayerFSM.IDLE, stateCmp.stateMachine.currentState)

        intentionCmp.walkDirection = WalkDirection.RIGHT
        stateCmp.stateMachine.update()
        assertEquals(PlayerFSM.WALK, stateCmp.stateMachine.currentState)
    }

    @Test
    fun `when jump is pressed state should be JUMP`() {
        val stateCmp = with(world) { entity[StateComponent] }
        val intentionCmp = with(world) { entity[IntentionComponent] }

        with(world) { entity.configure { it += HasGroundContact } }
        intentionCmp.wantsToJump = true
        stateCmp.stateMachine.update()
        assertEquals(PlayerFSM.JUMP, stateCmp.stateMachine.currentState)
    }

    @Test
    fun `when jump is double pressed state should be DOUBLE_JUMP`() {
        val stateCmp = with(world) { entity[StateComponent] }
        val intentionCmp = with(world) { entity[IntentionComponent] }

        with(world) { entity.configure { it += HasGroundContact } }
        intentionCmp.wantsToJump = true
        stateCmp.stateMachine.update()
        intentionCmp.wantsToJump = true
        stateCmp.stateMachine.update()
        assertEquals(PlayerFSM.DOUBLE_JUMP, stateCmp.stateMachine.currentState)
    }

    @Test
    fun `when bash is pressed from IDLE then state should be BASH`() {
        val stateCmp = with(world) { entity[StateComponent] }
        val intentionCmp = with(world) { entity[IntentionComponent] }

        intentionCmp.wantsToBash = true
        stateCmp.stateMachine.update()
        assertEquals(PlayerFSM.BASH, stateCmp.stateMachine.currentState)
    }

    @Test
    fun `when bash is pressed from JUMP then state should be BASH`() {
        val stateCmp = with(world) { entity[StateComponent] }
        val intentionCmp = with(world) { entity[IntentionComponent] }

        with(world) { entity.configure { it += HasGroundContact } }
        intentionCmp.wantsToJump = true
        stateCmp.stateMachine.update()
        assertEquals(PlayerFSM.JUMP, stateCmp.stateMachine.currentState)

        intentionCmp.wantsToBash = true
        stateCmp.stateMachine.update()
        assertEquals(PlayerFSM.BASH, stateCmp.stateMachine.currentState)
    }

    @Test
    fun `should not allow transition from IDLE to ATTACK_2`() {
        val stateCmp = with(world) { entity[StateComponent] }
        val intentionCmp = with(world) { entity[IntentionComponent] }
        givenState(PlayerFSM.IDLE)

        intentionCmp.wantsToAttack = true
        stateCmp.stateMachine.update()
        stateCmp.stateMachine.update()
        assertEquals(PlayerFSM.ATTACK_1, stateCmp.stateMachine.currentState)
    }

    @Test
    fun `should not allow transition from DOUBLE_JUMP to JUMP`() {
        val stateCmp = with(world) { entity[StateComponent] }
        val intentionCmp = with(world) { entity[IntentionComponent] }

        with(world) { entity.configure { it += HasGroundContact } }
        intentionCmp.wantsToJump = true
        stateCmp.stateMachine.update()
        intentionCmp.wantsToJump = true
        stateCmp.stateMachine.update()
        assertEquals(PlayerFSM.DOUBLE_JUMP, stateCmp.stateMachine.currentState)

        world.update(0.2f)
        intentionCmp.wantsToJump = true
        stateCmp.stateMachine.update()
        assertEquals(PlayerFSM.IDLE, stateCmp.stateMachine.currentState)
    }

    @Test
    fun `should not allow transition from DOUBLE_JUMP to IDLE without 100ms delay`() {
        val stateCmp = with(world) { entity[StateComponent] }
        val intentionCmp = with(world) { entity[IntentionComponent] }

        with(world) { entity.configure { it += HasGroundContact } }
        intentionCmp.wantsToJump = true
        stateCmp.stateMachine.update()
        intentionCmp.wantsToJump = true
        stateCmp.stateMachine.update()
        assertEquals(PlayerFSM.DOUBLE_JUMP, stateCmp.stateMachine.currentState)
        stateCmp.stateMachine.update()
        assertEquals(PlayerFSM.DOUBLE_JUMP, stateCmp.stateMachine.currentState)
        world.update(0.2f)
        stateCmp.stateMachine.update()

        assertEquals(PlayerFSM.IDLE, stateCmp.stateMachine.currentState)
    }

    @Test
    fun `when crouch is pressed then state should be CROUCH_IDLE`() {
        val stateCmp = with(world) { entity[StateComponent] }
        val intentionCmp = with(world) { entity[IntentionComponent] }

        intentionCmp.wantsToCrouch = true
        stateCmp.stateMachine.update()
        assertEquals(PlayerFSM.CROUCH_IDLE, stateCmp.stateMachine.currentState)
    }

    @Test
    fun `when crouch is pressed when walking then state should be CROUCH_WALK`() {
        val stateCmp = with(world) { entity[StateComponent] }
        val intentionCmp = with(world) { entity[IntentionComponent] }

        intentionCmp.walkDirection = WalkDirection.LEFT
        stateCmp.stateMachine.update()
        intentionCmp.wantsToCrouch = true
        stateCmp.stateMachine.update()
        assertEquals(PlayerFSM.CROUCH_WALK, stateCmp.stateMachine.currentState)
    }

    @Test
    fun `when jump is pressed and then state should be FALLING`() {
        val stateCmp = with(world) { entity[StateComponent] }
        val intentionCmp = with(world) { entity[IntentionComponent] }

        intentionCmp.wantsToJump = true
        with(world) { entity.configure { it += HasGroundContact } }
        stateCmp.stateMachine.update()
        assertEquals(PlayerFSM.JUMP, stateCmp.stateMachine.currentState)

        setNegativeYVelocity()
        with(world) { entity.configure { it -= HasGroundContact } }
        stateCmp.stateMachine.update()
        assertEquals(PlayerFSM.FALL, stateCmp.stateMachine.currentState)
    }

    @Test
    fun `crouch when jumping should not be possible`() {
        val stateCmp = with(world) { entity[StateComponent] }
        val intentionCmp = with(world) { entity[IntentionComponent] }

        with(world) { entity.configure { it += HasGroundContact } }
        intentionCmp.wantsToJump = true
        stateCmp.stateMachine.update()
        assertEquals(PlayerFSM.JUMP, stateCmp.stateMachine.currentState)

        intentionCmp.wantsToCrouch = true
        stateCmp.stateMachine.update()
        assertEquals(PlayerFSM.JUMP, stateCmp.stateMachine.currentState)
    }

    @Test
    fun `jump when crouching should not be possible`() {
        val stateCmp = with(world) { entity[StateComponent] }
        val intentionCmp = with(world) { entity[IntentionComponent] }

        intentionCmp.wantsToCrouch = true
        stateCmp.stateMachine.update()
        assertEquals(PlayerFSM.CROUCH_IDLE, stateCmp.stateMachine.currentState)

        intentionCmp.wantsToJump = true
        stateCmp.stateMachine.update()
        assertEquals(PlayerFSM.CROUCH_IDLE, stateCmp.stateMachine.currentState)
    }

    @Test
    fun `attack sequence from ATTACK_1 to ATTACK_3 then IDLE`() {
        val stateCmp = with(world) { entity[StateComponent] }
        val intentionCmp = with(world) { entity[IntentionComponent] }
        val animationCmp = with(world) { entity[AnimationComponent] }
        givenAnimationIsFinished(animationCmp)

        intentionCmp.wantsToAttack = true
        stateCmp.stateMachine.update()
        assertEquals(PlayerFSM.ATTACK_1, stateCmp.stateMachine.currentState)

        intentionCmp.wantsToAttack = true
        stateCmp.stateMachine.update()
        assertEquals(PlayerFSM.ATTACK_2, stateCmp.stateMachine.currentState)

        intentionCmp.wantsToAttack = true
        stateCmp.stateMachine.update()
        assertEquals(PlayerFSM.ATTACK_3, stateCmp.stateMachine.currentState)

        intentionCmp.wantsToAttack = false
        stateCmp.stateMachine.update()
        assertEquals(PlayerFSM.IDLE, stateCmp.stateMachine.currentState)
    }

    @Test
    fun `should not allow ATTACK_2 from JUMP`() {
        val stateCmp = with(world) { entity[StateComponent] }
        val intentionCmp = with(world) { entity[IntentionComponent] }

        with(world) { entity.configure { it += HasGroundContact } }
        intentionCmp.wantsToJump = true
        stateCmp.stateMachine.update()
        assertEquals(PlayerFSM.JUMP, stateCmp.stateMachine.currentState)

        intentionCmp.wantsToAttack = true
        stateCmp.stateMachine.update()
        assertNotEquals(PlayerFSM.ATTACK_2, stateCmp.stateMachine.currentState)
    }

    @Test
    fun `should transition from FALL to IDLE when landing`() {
        val stateCmp = with(world) { entity[StateComponent] }
        givenState(PlayerFSM.FALL)

        givenZeroVelocity()
        with(world) { entity.configure { it += HasGroundContact } }
        stateCmp.stateMachine.update()
        assertEquals(PlayerFSM.IDLE, stateCmp.stateMachine.currentState)
    }

    @Test
    fun `should NOT transition from FALL to IDLE when yVel is still above threshold`() {
        val stateCmp = with(world) { entity[StateComponent] }
        givenState(PlayerFSM.FALL)

        givenYVelocityAboveThreshold()
        with(world) { entity.configure { it += HasGroundContact } }
        stateCmp.stateMachine.update()
        assertEquals(PlayerFSM.FALL, stateCmp.stateMachine.currentState)
    }

    @Test
    fun `should not transition from FALL to DOUBLE_JUMP when jump pressed but grace is 0`() {
        val stateCmp = with(world) { entity[StateComponent] }
        val inputCmp = with(world) { entity[InputComponent] }
        val jumpCmp = with(world) { entity[JumpComponent] }
        jumpCmp.disableDoubleJumpGraceTimer()
        givenState(PlayerFSM.FALL)

        inputCmp.jumpJustPressed = true
        with(world) { entity.configure { it += HasGroundContact } }
        stateCmp.stateMachine.update()
        assertEquals(PlayerFSM.IDLE, stateCmp.stateMachine.currentState)
    }

    @Test
    fun `should transition from FALL to DOUBLE_JUMP when jump pressed and grace is gt 0`() {
        val stateCmp = with(world) { entity[StateComponent] }
        val intentionCmp = with(world) { entity[IntentionComponent] }
        val jumpCmp = with(world) { entity[JumpComponent] }
        jumpCmp.resetDoubleJumpGraceTimer()
        givenState(PlayerFSM.JUMP)
        stateCmp.stateMachine.update()
        givenState(PlayerFSM.FALL)

        intentionCmp.wantsToJump = true
        stateCmp.stateMachine.update()
        assertEquals(PlayerFSM.DOUBLE_JUMP, stateCmp.stateMachine.currentState)
    }

    @Test
    fun `should transition from BASH to IDLE when animation finished`() {
        val stateCmp = with(world) { entity[StateComponent] }
        val animationCmp = with(world) { entity[AnimationComponent] }
        givenAnimationIsFinished(animationCmp)
        givenState(PlayerFSM.BASH)

        stateCmp.stateMachine.update()
        assertEquals(PlayerFSM.IDLE, stateCmp.stateMachine.currentState)
    }

    @Test
    fun `should NOT transition from BASH to JUMP when animation finished and jump pressed`() {
        val stateCmp = with(world) { entity[StateComponent] }
        val inputCmp = with(world) { entity[InputComponent] }
        val animationCmp = with(world) { entity[AnimationComponent] }
        givenAnimationIsFinished(animationCmp)
        givenState(PlayerFSM.BASH)

        inputCmp.jumpJustPressed = true
        stateCmp.stateMachine.update()
        assertEquals(PlayerFSM.IDLE, stateCmp.stateMachine.currentState)
    }

    @Test
    fun `should transition from CROUCH_IDLE to IDLE when crouch released`() {
        val stateCmp = with(world) { entity[StateComponent] }
        val intentionCmp = with(world) { entity[IntentionComponent] }

        intentionCmp.wantsToCrouch = true
        stateCmp.stateMachine.update()
        assertEquals(PlayerFSM.CROUCH_IDLE, stateCmp.stateMachine.currentState)

        intentionCmp.wantsToCrouch = false
        stateCmp.stateMachine.update()
        assertEquals(PlayerFSM.IDLE, stateCmp.stateMachine.currentState)
    }

    @Test
    fun `should transition from CROUCH_WALK to CROUCH_IDLE when direction is NONE`() {
        val stateCmp = with(world) { entity[StateComponent] }
        val intentionCmp = with(world) { entity[IntentionComponent] }

        intentionCmp.wantsToCrouch = true
        intentionCmp.walkDirection = WalkDirection.LEFT
        stateCmp.stateMachine.update()
        assertEquals(PlayerFSM.CROUCH_WALK, stateCmp.stateMachine.currentState)

        intentionCmp.walkDirection = WalkDirection.NONE
        stateCmp.stateMachine.update()
        assertEquals(PlayerFSM.CROUCH_IDLE, stateCmp.stateMachine.currentState)
    }

    @Test
    fun `should transition from WALK to FALL when falling`() {
        val stateCmp = with(world) { entity[StateComponent] }
        val intentionCmp = with(world) { entity[IntentionComponent] }

        intentionCmp.walkDirection = WalkDirection.RIGHT
        stateCmp.stateMachine.update()
        assertEquals(PlayerFSM.WALK, stateCmp.stateMachine.currentState)

        setNegativeYVelocity()
        stateCmp.stateMachine.update()
        assertEquals(PlayerFSM.FALL, stateCmp.stateMachine.currentState)
    }

    @Test
    fun `should transition from ATTACK_1 to FALL when falling`() {
        val stateCmp = with(world) { entity[StateComponent] }
        val intentionCmp = with(world) { entity[IntentionComponent] }
        val animationCmp = with(world) { entity[AnimationComponent] }
        givenAnimationIsFinished(animationCmp)

        intentionCmp.wantsToAttack = true
        stateCmp.stateMachine.update()
        assertEquals(PlayerFSM.ATTACK_1, stateCmp.stateMachine.currentState)

        setNegativeYVelocity()
        stateCmp.stateMachine.update()
        assertEquals(PlayerFSM.FALL, stateCmp.stateMachine.currentState)
    }

    @Test
    fun `should transition from ATTACK_2 to FALL when falling`() {
        val stateCmp = with(world) { entity[StateComponent] }
        val intentionCmp = with(world) { entity[IntentionComponent] }
        val animationCmp = with(world) { entity[AnimationComponent] }
        givenAnimationIsFinished(animationCmp)

        intentionCmp.wantsToAttack = true
        stateCmp.stateMachine.update()

        intentionCmp.wantsToAttack2 = true
        stateCmp.stateMachine.update()
        assertEquals(PlayerFSM.ATTACK_2, stateCmp.stateMachine.currentState)

        setNegativeYVelocity()
        stateCmp.stateMachine.update()
        assertEquals(PlayerFSM.FALL, stateCmp.stateMachine.currentState)
    }

    @Test
    fun `should transition from DOUBLE_JUMP to BASH when bash pressed`() {
        val stateCmp = with(world) { entity[StateComponent] }
        val intentionCmp = with(world) { entity[IntentionComponent] }
        with(world) { entity.configure { it += HasGroundContact } }

        intentionCmp.wantsToJump = true
        stateCmp.stateMachine.update()
        intentionCmp.wantsToJump = true
        stateCmp.stateMachine.update()
        assertEquals(PlayerFSM.DOUBLE_JUMP, stateCmp.stateMachine.currentState)

        stateCmp.stateMachine.update()
        world.update(0.3f)
        intentionCmp.wantsToBash = true
        stateCmp.stateMachine.update()
        assertEquals(PlayerFSM.BASH, stateCmp.stateMachine.currentState)
    }

    @Test
    fun `should transition from ATTACK_3 to IDLE when animation finished`() {
        val stateCmp = with(world) { entity[StateComponent] }
        val animationCmp = with(world) { entity[AnimationComponent] }
        givenAnimationIsFinished(animationCmp)
        givenState(PlayerFSM.ATTACK_3)

        stateCmp.stateMachine.update()
        assertEquals(PlayerFSM.IDLE, stateCmp.stateMachine.currentState)
    }

    @Test
    fun `should not allow transition from DOUBLE_JUMP to CROUCH_WALK`() {
        val stateCmp = with(world) { entity[StateComponent] }
        val inputCmp = with(world) { entity[InputComponent] }
        givenState(PlayerFSM.DOUBLE_JUMP)

        inputCmp.crouchJustPressed = true
        inputCmp.walkRightJustPressed = true
        stateCmp.stateMachine.update()
        assertEquals(PlayerFSM.DOUBLE_JUMP, stateCmp.stateMachine.currentState)
    }

    @Test
    fun `should not transition to ATTACK_2 if animation not finished`() {
        val stateCmp = with(world) { entity[StateComponent] }
        val intentionCmp = with(world) { entity[IntentionComponent] }
        val animationCmp = with(world) { entity[AnimationComponent] }

        every { animationCmp.animation.isAnimationFinished(any()) } returns false
        intentionCmp.wantsToAttack = true
        stateCmp.stateMachine.update()

        assertEquals(PlayerFSM.ATTACK_1, stateCmp.stateMachine.currentState)

        intentionCmp.wantsToAttack = true
        stateCmp.stateMachine.update()
        assertEquals(PlayerFSM.ATTACK_1, stateCmp.stateMachine.currentState)
    }

    @Test
    fun `should remain in IDLE if no input given`() {
        val stateCmp = with(world) { entity[StateComponent] }

        repeat(3) {
            stateCmp.stateMachine.update()
            assertEquals(PlayerFSM.IDLE, stateCmp.stateMachine.currentState)
        }
    }

    @Test
    fun `should not chain to ATTACK_2 if no additional attack input`() {
        val stateCmp = with(world) { entity[StateComponent] }
        val intentionCmp = with(world) { entity[IntentionComponent] }
        val animationCmp = with(world) { entity[AnimationComponent] }
        givenAnimationIsFinished(animationCmp)

        intentionCmp.wantsToAttack = true
        stateCmp.stateMachine.update()
        assertEquals(PlayerFSM.ATTACK_1, stateCmp.stateMachine.currentState)

        stateCmp.stateMachine.update()

        assertEquals(PlayerFSM.IDLE, stateCmp.stateMachine.currentState)
    }

    @Test
    fun `should transition from CROUCH_IDLE to WALK when crouch released and direction is not NONE`() {
        val stateCmp = with(world) { entity[StateComponent] }
        val intentionCmp = with(world) { entity[IntentionComponent] }

        intentionCmp.wantsToCrouch = true
        stateCmp.stateMachine.update()
        assertEquals(PlayerFSM.CROUCH_IDLE, stateCmp.stateMachine.currentState)

        intentionCmp.wantsToCrouch = false
        intentionCmp.walkDirection = WalkDirection.LEFT
        stateCmp.stateMachine.update()
        assertEquals(PlayerFSM.WALK, stateCmp.stateMachine.currentState)
    }

    @Test
    fun `should transition from CROUCH_WALK to IDLE when crouch released and direction is NONE`() {
        val stateCmp = with(world) { entity[StateComponent] }
        val intentionCmp = with(world) { entity[IntentionComponent] }

        intentionCmp.wantsToCrouch = true
        intentionCmp.walkDirection = WalkDirection.LEFT
        stateCmp.stateMachine.update()
        assertEquals(PlayerFSM.CROUCH_WALK, stateCmp.stateMachine.currentState)

        intentionCmp.wantsToCrouch = false
        intentionCmp.walkDirection = WalkDirection.NONE
        stateCmp.stateMachine.update()
        assertEquals(PlayerFSM.IDLE, stateCmp.stateMachine.currentState)
    }

    @Test
    fun `should remain in FALL if still falling`() {
        val stateCmp = with(world) { entity[StateComponent] }
        givenState(PlayerFSM.FALL)
        setNegativeYVelocity()

        stateCmp.stateMachine.update()
        assertEquals(PlayerFSM.FALL, stateCmp.stateMachine.currentState)
    }

    @Test
    fun `should switch to DEATH state when health is 0`() {
        val stateCmp = with(world) { entity[StateComponent] }
        val healthCmp = with(world) { entity[HealthComponent] }

        givenState(PlayerFSM.IDLE)
        healthCmp.current = 0f

        stateCmp.stateMachine.update()
        assertEquals(PlayerFSM.DEATH, stateCmp.stateMachine.currentState)
    }

    @Test
    fun `should not change state when in DEATH state`() {
        val stateCmp = with(world) { entity[StateComponent] }
        val inputCmp = with(world) { entity[InputComponent] }
        givenState(PlayerFSM.DEATH)
        with(world) { entity.configure { it += HasGroundContact } }

        inputCmp.jumpJustPressed = true
        stateCmp.stateMachine.update()
        world.update(1f)
        assertNotEquals(PlayerFSM.JUMP, stateCmp.stateMachine.currentState)
    }

    @Test
    fun `should change state to RESURRECT when in DEATH state`() {
        val messageDispatcher = MessageManager.getInstance()
        val stateCmp = with(world) { entity[StateComponent] }
        givenState(PlayerFSM.DEATH)

        messageDispatcher.dispatchMessage(
            0f,
            FsmMessageTypes.KILL.ordinal,
            true,
        )
        stateCmp.stateMachine.update()
        world.update(1f)
        assertNotEquals(PlayerFSM.RESURRECT, stateCmp.stateMachine.currentState)
    }

    @Test
    fun `death state schedules removal and deactivates body`() {
        val deadDelay = 0f
        val deadCmp = with(world) { entity[DeadComponent] }
        val healthCmp = with(world) { entity[HealthComponent] }
        givenState(PlayerFSM.DEATH)

        assertTrue(healthCmp.isDead)
        assertFalse(bodyMock.isActive)

        assertEquals(deadDelay, deadCmp.removeDelayCounter, 1e-4f)
    }

    private fun givenState(state: PlayerFSM) {
        @Suppress("UNCHECKED_CAST")
        val stateCmp: StateComponent<PlayerStateContext, PlayerFSM> =
            with(world) {
                entity[StateComponent] as
                    StateComponent<PlayerStateContext, PlayerFSM>
            }
        stateCmp.changeState(state)
    }

    private fun givenAnimationIsFinished(animationCmp: AnimationComponent) {
        every { animationCmp.animation.isAnimationFinished(any()) } returns true
    }

    private fun givenZeroVelocity() {
        val velocity = Vector2(0f, 0f)
        every { bodyMock.linearVelocity } returns velocity
    }

    private fun givenYVelocityAboveThreshold() {
        val velocity = Vector2(0f, -.11f)
        every { bodyMock.linearVelocity } returns velocity
    }

    private fun setNegativeYVelocity() {
        val velocity = Vector2(0f, -5f)
        every { bodyMock.linearVelocity } returns velocity
    }
}
