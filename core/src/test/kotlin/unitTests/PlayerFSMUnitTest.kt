package unitTests

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.Body
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import com.github.quillraven.fleks.configureWorld
import io.bennyoe.ai.PlayerFSM
import io.bennyoe.ai.StateContext
import io.bennyoe.components.AiComponent
import io.bennyoe.components.AnimationComponent
import io.bennyoe.components.HasGroundContact
import io.bennyoe.components.InputComponent
import io.bennyoe.components.JumpComponent
import io.bennyoe.components.MoveComponent
import io.bennyoe.components.PhysicComponent
import io.bennyoe.components.WalkDirection
import io.bennyoe.systems.AiSystem
import io.bennyoe.systems.MoveSystem
import io.mockk.every
import io.mockk.mockk
import ktx.collections.gdxArrayOf
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class PlayerFSMUnitTest {
    private lateinit var world: World
    private lateinit var entity: Entity
    private lateinit var stateContext: StateContext
    private lateinit var bodyMock: Body

    @BeforeEach
    fun setup() {
        val mockApp = mockk<Application>(relaxed = true)
        Gdx.app = mockApp

        bodyMock = mockk<Body>(relaxed = true)

        val atlasMock = mockk<TextureAtlas>(relaxed = true)
        val animationMock = mockk<Animation<TextureRegionDrawable>>(relaxed = true)
        val regionMock = mockk<TextureAtlas.AtlasRegion>(relaxed = true)

        every { atlasMock.findRegions(any()) } returns gdxArrayOf(regionMock)
        every { animationMock.isAnimationFinished(any()) } returns false

        val animationComponent =
            AnimationComponent().apply {
                animation = animationMock
            }

        world =
            configureWorld {
                systems {
                    add(MoveSystem())
                    add(AiSystem())
                }
            }

        entity =
            world.entity {
                val physicCmp = PhysicComponent()
                physicCmp.body = bodyMock
                it += physicCmp
                it += MoveComponent(maxSpeed = 10f)
                it += InputComponent()
                it += animationComponent
                it += JumpComponent()
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

        with(world) { entity.configure { it += HasGroundContact } }
        inputComponent.jumpJustPressed = true
        aiComponent.stateMachine.update()
        assertEquals(PlayerFSM.JUMP, aiComponent.stateMachine.currentState)
    }

    @Test
    fun `when jump is double pressed state should be DOUBLE_JUMP`() {
        val aiComponent = with(world) { entity[AiComponent] }
        val inputComponent = with(world) { entity[InputComponent] }

        with(world) { entity.configure { it += HasGroundContact } }
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

        with(world) { entity.configure { it += HasGroundContact } }
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
        givenState(PlayerFSM.IDLE)

        inputComponent.attackJustPressed = true
        aiComponent.stateMachine.update()
        aiComponent.stateMachine.update()
        assertEquals(PlayerFSM.ATTACK_1, aiComponent.stateMachine.currentState)
    }

    @Test
    fun `should not allow transition from DOUBLE_JUMP to JUMP`() {
        val aiComponent = with(world) { entity[AiComponent] }
        val inputComponent = with(world) { entity[InputComponent] }

        with(world) { entity.configure { it += HasGroundContact } }
        inputComponent.jumpJustPressed = true
        aiComponent.stateMachine.update()
        inputComponent.jumpJustPressed = true
        aiComponent.stateMachine.update()
        assertEquals(PlayerFSM.DOUBLE_JUMP, aiComponent.stateMachine.currentState)

        inputComponent.jumpJustPressed = true
        aiComponent.stateMachine.update()
        assertEquals(PlayerFSM.IDLE, aiComponent.stateMachine.currentState)
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
        with(world) { entity.configure { it += HasGroundContact } }
        aiComponent.stateMachine.update()
        assertEquals(PlayerFSM.JUMP, aiComponent.stateMachine.currentState)

        setNegativeYVelocity()
        with(world) { entity.configure { it -= HasGroundContact } }
        aiComponent.stateMachine.update()
        assertEquals(PlayerFSM.FALL, aiComponent.stateMachine.currentState)
    }

    @Test
    fun `crouch when jumping should not be possible`() {
        val aiComponent = with(world) { entity[AiComponent] }
        val inputComponent = with(world) { entity[InputComponent] }

        with(world) { entity.configure { it += HasGroundContact } }
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
        givenAnimationIsFinished(animationComponent)

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

        with(world) { entity.configure { it += HasGroundContact } }
        inputComponent.jumpJustPressed = true
        aiComponent.stateMachine.update()
        assertEquals(PlayerFSM.JUMP, aiComponent.stateMachine.currentState)

        inputComponent.attackJustPressed = true
        aiComponent.stateMachine.update()
        assertNotEquals(PlayerFSM.ATTACK_2, aiComponent.stateMachine.currentState)
    }

    @Test
    fun `should transition from FALL to IDLE when landing`() {
        val aiComponent = with(world) { entity[AiComponent] }
        givenState(PlayerFSM.FALL)

        givenZeroVelocity()
        with(world) { entity.configure { it += HasGroundContact } }
        aiComponent.stateMachine.update()
        assertEquals(PlayerFSM.IDLE, aiComponent.stateMachine.currentState)
    }

    @Test
    fun `should NOT transition from FALL to IDLE when yVel is still above threshold`() {
        val aiComponent = with(world) { entity[AiComponent] }
        givenState(PlayerFSM.FALL)

        givenYVelocityAboveThreshold()
        with(world) { entity.configure { it += HasGroundContact } }
        aiComponent.stateMachine.update()
        assertEquals(PlayerFSM.FALL, aiComponent.stateMachine.currentState)
    }

    @Test
    fun `should not transition from FALL to DOUBLE_JUMP when jump pressed but grace is 0`() {
        val aiComponent = with(world) { entity[AiComponent] }
        val inputComponent = with(world) { entity[InputComponent] }
        val jumpComponent = with(world) { entity[JumpComponent] }
        jumpComponent.disableDoubleJumpGraceTimer()
        givenState(PlayerFSM.FALL)

        inputComponent.jumpJustPressed = true
        with(world) { entity.configure { it += HasGroundContact } }
        aiComponent.stateMachine.update()
        assertEquals(PlayerFSM.IDLE, aiComponent.stateMachine.currentState)
    }

    @Test
    fun `should transition from FALL to DOUBLE_JUMP when jump pressed and grace is gt 0`() {
        val aiComponent = with(world) { entity[AiComponent] }
        val inputComponent = with(world) { entity[InputComponent] }
        val jumpComponent = with(world) { entity[JumpComponent] }
        jumpComponent.resetDoubleJumpGraceTimer()
        givenState(PlayerFSM.JUMP)
        aiComponent.stateMachine.update()
        givenState(PlayerFSM.FALL)

        inputComponent.jumpJustPressed = true
        aiComponent.stateMachine.update()
        assertEquals(PlayerFSM.DOUBLE_JUMP, aiComponent.stateMachine.currentState)
    }

    @Test
    fun `should transition from BASH to IDLE when animation finished`() {
        val aiComponent = with(world) { entity[AiComponent] }
        val animationComponent = with(world) { entity[AnimationComponent] }
        givenAnimationIsFinished(animationComponent)
        givenState(PlayerFSM.BASH)

        aiComponent.stateMachine.update()
        assertEquals(PlayerFSM.IDLE, aiComponent.stateMachine.currentState)
    }

    @Test
    fun `should NOT transition from BASH to JUMP when animation finished and jump pressed`() {
        val aiComponent = with(world) { entity[AiComponent] }
        val inputComponent = with(world) { entity[InputComponent] }
        val animationComponent = with(world) { entity[AnimationComponent] }
        givenAnimationIsFinished(animationComponent)
        givenState(PlayerFSM.BASH)

        inputComponent.jumpJustPressed = true
        aiComponent.stateMachine.update()
        assertEquals(PlayerFSM.IDLE, aiComponent.stateMachine.currentState)
    }

    @Test
    fun `should transition from CROUCH_IDLE to IDLE when crouch released`() {
        val aiComponent = with(world) { entity[AiComponent] }
        val inputComponent = with(world) { entity[InputComponent] }

        inputComponent.crouch = true
        aiComponent.stateMachine.update()
        assertEquals(PlayerFSM.CROUCH_IDLE, aiComponent.stateMachine.currentState)

        inputComponent.crouch = false
        aiComponent.stateMachine.update()
        assertEquals(PlayerFSM.IDLE, aiComponent.stateMachine.currentState)
    }

    @Test
    fun `should transition from CROUCH_WALK to CROUCH_IDLE when direction is NONE`() {
        val aiComponent = with(world) { entity[AiComponent] }
        val inputComponent = with(world) { entity[InputComponent] }

        inputComponent.crouch = true
        inputComponent.direction = WalkDirection.LEFT
        aiComponent.stateMachine.update()
        assertEquals(PlayerFSM.CROUCH_WALK, aiComponent.stateMachine.currentState)

        inputComponent.direction = WalkDirection.NONE
        aiComponent.stateMachine.update()
        assertEquals(PlayerFSM.CROUCH_IDLE, aiComponent.stateMachine.currentState)
    }

    @Test
    fun `should transition from WALK to FALL when falling`() {
        val aiComponent = with(world) { entity[AiComponent] }
        val inputComponent = with(world) { entity[InputComponent] }

        inputComponent.direction = WalkDirection.RIGHT
        aiComponent.stateMachine.update()
        assertEquals(PlayerFSM.WALK, aiComponent.stateMachine.currentState)

        setNegativeYVelocity()
        aiComponent.stateMachine.update()
        assertEquals(PlayerFSM.FALL, aiComponent.stateMachine.currentState)
    }

    @Test
    fun `should transition from ATTACK_1 to FALL when falling`() {
        val aiComponent = with(world) { entity[AiComponent] }
        val inputComponent = with(world) { entity[InputComponent] }
        val animationComponent = with(world) { entity[AnimationComponent] }
        givenAnimationIsFinished(animationComponent)

        inputComponent.attackJustPressed = true
        aiComponent.stateMachine.update()
        assertEquals(PlayerFSM.ATTACK_1, aiComponent.stateMachine.currentState)

        setNegativeYVelocity()
        aiComponent.stateMachine.update()
        assertEquals(PlayerFSM.FALL, aiComponent.stateMachine.currentState)
    }

    @Test
    fun `should transition from ATTACK_2 to FALL when falling`() {
        val aiComponent = with(world) { entity[AiComponent] }
        val inputComponent = with(world) { entity[InputComponent] }
        val animationComponent = with(world) { entity[AnimationComponent] }
        givenAnimationIsFinished(animationComponent)

        inputComponent.attackJustPressed = true
        aiComponent.stateMachine.update()

        inputComponent.attack2JustPressed = true
        aiComponent.stateMachine.update()
        assertEquals(PlayerFSM.ATTACK_2, aiComponent.stateMachine.currentState)

        setNegativeYVelocity()
        aiComponent.stateMachine.update()
        assertEquals(PlayerFSM.FALL, aiComponent.stateMachine.currentState)
    }

    @Test
    fun `should transition from DOUBLE_JUMP to BASH when bash pressed`() {
        val aiComponent = with(world) { entity[AiComponent] }
        val inputComponent = with(world) { entity[InputComponent] }
        with(world) { entity.configure { it += HasGroundContact } }

        inputComponent.jumpJustPressed = true
        aiComponent.stateMachine.update()
        inputComponent.jumpJustPressed = true
        aiComponent.stateMachine.update()
        assertEquals(PlayerFSM.DOUBLE_JUMP, aiComponent.stateMachine.currentState)

        inputComponent.bashJustPressed = true
        aiComponent.stateMachine.update()
        assertEquals(PlayerFSM.BASH, aiComponent.stateMachine.currentState)
    }

    @Test
    fun `should transition from ATTACK_3 to IDLE when animation finished`() {
        val aiComponent = with(world) { entity[AiComponent] }
        val animationComponent = with(world) { entity[AnimationComponent] }
        givenAnimationIsFinished(animationComponent)
        givenState(PlayerFSM.ATTACK_3)

        aiComponent.stateMachine.update()
        assertEquals(PlayerFSM.IDLE, aiComponent.stateMachine.currentState)
    }

    @Test
    fun `should not allow transition from DOUBLE_JUMP to CROUCH_WALK`() {
        val aiComponent = with(world) { entity[AiComponent] }
        val inputComponent = with(world) { entity[InputComponent] }
        givenState(PlayerFSM.DOUBLE_JUMP)

        inputComponent.crouch = true
        inputComponent.direction = WalkDirection.RIGHT
        aiComponent.stateMachine.update()
        assertEquals(PlayerFSM.DOUBLE_JUMP, aiComponent.stateMachine.currentState)
    }

    @Test
    fun `should not transition to ATTACK_2 if animation not finished`() {
        val aiComponent = with(world) { entity[AiComponent] }
        val inputComponent = with(world) { entity[InputComponent] }
        val animationComponent = with(world) { entity[AnimationComponent] }

        every { animationComponent.animation.isAnimationFinished(any()) } returns false
        inputComponent.attackJustPressed = true
        aiComponent.stateMachine.update()

        assertEquals(PlayerFSM.ATTACK_1, aiComponent.stateMachine.currentState)

        inputComponent.attackJustPressed = true
        aiComponent.stateMachine.update()
        assertEquals(PlayerFSM.ATTACK_1, aiComponent.stateMachine.currentState)
    }

    @Test
    fun `should remain in IDLE if no input given`() {
        val aiComponent = with(world) { entity[AiComponent] }

        repeat(3) {
            aiComponent.stateMachine.update()
            assertEquals(PlayerFSM.IDLE, aiComponent.stateMachine.currentState)
        }
    }

    @Test
    fun `should not chain to ATTACK_2 if no additional attack input`() {
        val aiComponent = with(world) { entity[AiComponent] }
        val inputComponent = with(world) { entity[InputComponent] }
        val animationComponent = with(world) { entity[AnimationComponent] }
        givenAnimationIsFinished(animationComponent)

        inputComponent.attackJustPressed = true
        aiComponent.stateMachine.update()
        assertEquals(PlayerFSM.ATTACK_1, aiComponent.stateMachine.currentState)

        aiComponent.stateMachine.update()

        assertEquals(PlayerFSM.IDLE, aiComponent.stateMachine.currentState)
    }

    @Test
    fun `should transition from CROUCH_IDLE to WALK when crouch released and direction is not NONE`() {
        val aiComponent = with(world) { entity[AiComponent] }
        val inputComponent = with(world) { entity[InputComponent] }

        inputComponent.crouch = true
        aiComponent.stateMachine.update()
        assertEquals(PlayerFSM.CROUCH_IDLE, aiComponent.stateMachine.currentState)

        inputComponent.crouch = false
        inputComponent.direction = WalkDirection.RIGHT
        aiComponent.stateMachine.update()
        assertEquals(PlayerFSM.WALK, aiComponent.stateMachine.currentState)
    }

    @Test
    fun `should transition from CROUCH_WALK to IDLE when crouch released and direction is NONE`() {
        val aiComponent = with(world) { entity[AiComponent] }
        val inputComponent = with(world) { entity[InputComponent] }

        inputComponent.crouch = true
        inputComponent.direction = WalkDirection.LEFT
        aiComponent.stateMachine.update()
        assertEquals(PlayerFSM.CROUCH_WALK, aiComponent.stateMachine.currentState)

        inputComponent.crouch = false
        inputComponent.direction = WalkDirection.NONE
        aiComponent.stateMachine.update()
        assertEquals(PlayerFSM.IDLE, aiComponent.stateMachine.currentState)
    }

    @Test
    fun `should remain in FALL if still falling`() {
        val aiComponent = with(world) { entity[AiComponent] }
        givenState(PlayerFSM.FALL)
        setNegativeYVelocity()

        aiComponent.stateMachine.update()
        assertEquals(PlayerFSM.FALL, aiComponent.stateMachine.currentState)
    }

    private fun givenState(state: PlayerFSM) {
        val aiComponent = with(world) { entity[AiComponent] }
        aiComponent.stateMachine.changeState(state)
    }

    private fun givenAnimationIsFinished(animationComponent: AnimationComponent) {
        every { animationComponent.animation.isAnimationFinished(any()) } returns true
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
