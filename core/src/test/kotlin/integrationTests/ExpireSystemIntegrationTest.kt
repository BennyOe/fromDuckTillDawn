package integrationTests

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.physics.box2d.Body
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.github.bennyOe.gdxNormalLight.core.GameLight
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import com.github.quillraven.fleks.configureWorld
import io.bennyoe.components.AnimationComponent
import io.bennyoe.components.AttackComponent
import io.bennyoe.components.DeadComponent
import io.bennyoe.components.HealthComponent
import io.bennyoe.components.ImageComponent
import io.bennyoe.components.InputComponent
import io.bennyoe.components.IntentionComponent
import io.bennyoe.components.JumpComponent
import io.bennyoe.components.LightComponent
import io.bennyoe.components.MoveComponent
import io.bennyoe.components.PhysicComponent
import io.bennyoe.components.StateComponent
import io.bennyoe.state.player.PlayerCheckAliveState
import io.bennyoe.state.player.PlayerFSM
import io.bennyoe.state.player.PlayerStateContext
import io.bennyoe.systems.ExpireSystem
import io.bennyoe.systems.InputSystem
import io.bennyoe.systems.MoveSystem
import io.bennyoe.systems.StateSystem
import io.mockk.Called
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import com.badlogic.gdx.physics.box2d.World as Box2DWorld

class ExpireSystemIntegrationTest {
    private lateinit var world: World
    private lateinit var entity: Entity
    private lateinit var bodyMock: Body
    private lateinit var box2dWorldMock: Box2DWorld
    private lateinit var stage: Stage

    @BeforeEach
    fun setup() {
        Gdx.app = mockk<Application>(relaxed = true)
        val animationMock = mockk<Animation<TextureRegionDrawable>>(relaxed = true)
        bodyMock = mockk<Body>(relaxed = true)
        val stageMock = mockk<Stage>(relaxed = true)
        box2dWorldMock = mockk(relaxed = true)
        val gameLight = mockk<GameLight>(relaxed = true)
        stage = mockk<Stage>(relaxed = true)

        // Mock animation to return true for isAnimationFinished
        io.mockk.every { animationMock.isAnimationFinished(any()) } returns true

        // Set up bodyMock to return box2dWorldMock when body.world is accessed
        io.mockk.every { bodyMock.world } returns box2dWorldMock

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
                    add(ExpireSystem())
                }
            }

        entity =
            world.entity {
                it += AttackComponent()
                it += PhysicComponent().apply { body = bodyMock }
                it += MoveComponent(maxSpeed = 10f)
                it += HealthComponent(current = -1f)
                it += LightComponent(gameLight)
                it += IntentionComponent()
                it += InputComponent()
                it +=
                    DeadComponent(
                        false,
                        0.3f,
                        0.3f,
                    )
                it += JumpComponent()
                it += AnimationComponent().apply { animation = animationMock }
                it += imgCmp
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
    fun `expire system removes entity and destroys body after delay`() {
        val delay = 0.3f

        world.update(delay - 0.01f)
        assertTrue(world.contains(entity))
        verify { box2dWorldMock wasNot Called }

        world.update(0.05f)
        assertFalse(world.contains(entity))
        verify { box2dWorldMock.destroyBody(bodyMock) }
    }

    @Test
    fun `expire system keeps entity and destroys body after delay`() {
        val delay = 0.3f
        val deadCmp = with(world) { entity[DeadComponent] }
        val physicCmp = with(world) { entity[PhysicComponent] }
        deadCmp.keepCorpse = true

        world.update(delay - 0.01f)
        assertTrue(world.contains(entity))
        verify { box2dWorldMock wasNot Called }

        world.update(0.05f)
        assertTrue(world.contains(entity))
        assertFalse(physicCmp.body.isActive)
    }

    @Test
    fun `resurrect resets the removeDelay`() {
        val delay = 0.3f
        val deadCmp = with(world) { entity[DeadComponent] }

        world.update(delay - 0.01f)

        deadCmp.resetRemoveDealyCounter()

        assertEquals(delay, deadCmp.removeDelay)
    }
}
