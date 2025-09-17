package unitTests

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.TextureAtlas
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
import io.bennyoe.state.mushroom.MushroomCheckAliveState
import io.bennyoe.state.mushroom.MushroomFSM
import io.bennyoe.state.mushroom.MushroomStateContext
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

class MushroomFSMUnitTest {
    private lateinit var world: World
    private lateinit var entity: Entity
    private lateinit var stateContext: MushroomStateContext
    private lateinit var bodyMock: Body
    private lateinit var stage: Stage

    @BeforeEach
    fun setup() {
        val mockApp = mockk<Application>(relaxed = true)
        Gdx.app = mockApp

        bodyMock = mockk<Body>(relaxed = true)
        stage = mockk<Stage>(relaxed = true)

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
                it += MoveComponent(maxWalkSpeed = 10f)
                it += imgCmp
                it += InputComponent()
                it += animationCmp
                it +=
                    DeadComponent(
                        false,
                        0.3f,
                        0.3f,
                    )
                it += JumpComponent()
                it +=
                    StateComponent(
                        world,
                        MushroomStateContext(it, world, stage),
                        MushroomFSM.IDLE,
                        MushroomCheckAliveState,
                    )
            }
        stateContext = MushroomStateContext(entity, world, stage)
    }

    @Test
    fun `default state should be IDLE`() {
        val stateCmp = with(world) { entity[StateComponent] }
        assertEquals(MushroomFSM.IDLE, stateCmp.stateMachine.currentState)
    }

    @Test
    fun `when WalkDirection is not NONE then state should be WALK`() {
        val stateCmp = with(world) { entity[StateComponent] }
        val intentionCmp = with(world) { entity[IntentionComponent] }

        intentionCmp.walkDirection = WalkDirection.LEFT
        stateCmp.stateMachine.update()
        assertEquals(MushroomFSM.WALK, stateCmp.stateMachine.currentState)

        intentionCmp.walkDirection = WalkDirection.NONE
        stateCmp.stateMachine.update()
        assertEquals(MushroomFSM.IDLE, stateCmp.stateMachine.currentState)

        intentionCmp.walkDirection = WalkDirection.RIGHT
        stateCmp.stateMachine.update()
        assertEquals(MushroomFSM.WALK, stateCmp.stateMachine.currentState)
    }

    @Test
    fun `should remain in IDLE if no input given`() {
        val stateCmp = with(world) { entity[StateComponent] }

        repeat(3) {
            stateCmp.stateMachine.update()
            assertEquals(MushroomFSM.IDLE, stateCmp.stateMachine.currentState)
        }
    }

    @Test
    fun `should switch to DEATH state when health is 0`() {
        val stateCmp = with(world) { entity[StateComponent] }
        val healthCmp = with(world) { entity[HealthComponent] }

        givenState(MushroomFSM.IDLE)
        healthCmp.current = 0f

        stateCmp.stateMachine.update()
        assertEquals(MushroomFSM.DEATH, stateCmp.stateMachine.currentState)
    }

    @Test
    fun `should not change state when in DEATH state`() {
        val stateCmp = with(world) { entity[StateComponent] }
        val inputCmp = with(world) { entity[InputComponent] }
        givenState(MushroomFSM.DEATH)
        with(world) { entity.configure { it += HasGroundContact } }

        inputCmp.jumpJustPressed = true
        stateCmp.stateMachine.update()
        world.update(1f)
        assertNotEquals(MushroomFSM.WALK, stateCmp.stateMachine.currentState)
    }

    @Test
    fun `death state schedules removal and deactivates body`() {
        val deadDelay = 2f
        val deadCmp = with(world) { entity[DeadComponent] }
        val healthCmp = with(world) { entity[HealthComponent] }
        healthCmp.current = 0f
        deadCmp.removeDelayCounter = deadDelay
        deadCmp.removeDelay = deadDelay

        givenState(MushroomFSM.DEATH)

        assertTrue(healthCmp.isDead)
        assertFalse(bodyMock.isActive)

        assertEquals(deadDelay, deadCmp.removeDelay, 1e-4f)
    }

    private fun givenState(state: MushroomFSM) {
        @Suppress("UNCHECKED_CAST")
        val stateCmp: StateComponent<MushroomStateContext, MushroomFSM> =
            with(world) {
                entity[StateComponent] as
                    StateComponent<MushroomStateContext, MushroomFSM>
            }
        stateCmp.changeState(state)
    }
}
