package integrationTests

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.physics.box2d.Body
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import com.github.quillraven.fleks.configureWorld
import io.bennyoe.components.AnimationComponent
import io.bennyoe.components.AnimationType
import io.bennyoe.components.HasGroundContact
import io.bennyoe.components.HealthComponent
import io.bennyoe.components.InputComponent
import io.bennyoe.components.JumpComponent
import io.bennyoe.components.MoveComponent
import io.bennyoe.components.PhysicComponent
import io.bennyoe.components.StateComponent
import io.bennyoe.components.WalkDirection
import io.bennyoe.systems.AnimationSystem
import io.bennyoe.systems.MoveSystem
import io.bennyoe.systems.StateSystem
import io.mockk.every
import io.mockk.mockk
import ktx.collections.gdxArrayOf
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AnimationSystemIntegrationTest {
    private lateinit var world: World
    private lateinit var entity: Entity

    @BeforeEach
    fun setup() {
        Gdx.app = mockk<Application>(relaxed = true)
        val atlasMock = mockk<TextureAtlas>(relaxed = true)
        val animationMock = mockk<Animation<TextureRegionDrawable>>(relaxed = true)
        val regionMock = mockk<TextureAtlas.AtlasRegion>(relaxed = true)
        val bodyMock = mockk<Body>(relaxed = true)
        val animationComponent =
            AnimationComponent().apply {
                animation = animationMock
            }

        every { atlasMock.findRegions(any()) } returns gdxArrayOf(regionMock)
        every { animationMock.isAnimationFinished(any()) } returns false

        world =
            configureWorld {
                injectables {
                    add(atlasMock)
                }
                systems {
                    add(MoveSystem())
                    add(StateSystem())
                    add(AnimationSystem())
                }
            }

        entity =
            world.entity {
                it += MoveComponent()
                it += PhysicComponent().apply { body = bodyMock }
                it += HealthComponent()
                it += animationComponent
                it += JumpComponent()
                it += HasGroundContact
                it += InputComponent(direction = WalkDirection.NONE)
                it += StateComponent(world)
            }
    }

    @Test
    fun `no walk direktion sets animation type to IDLE`() {
        val animationCmp = with(world) { entity[AnimationComponent] }
        val inputCmp = with(world) { entity[InputComponent] }

        inputCmp.direction = WalkDirection.RIGHT
        world.update(0.016f)

        inputCmp.direction = WalkDirection.NONE
        world.update(0.016f)

        Assertions.assertEquals(
            AnimationType.IDLE,
            animationCmp.nextAnimationType,
            "Entity standing still should play idle animation",
        )
    }

    /**
     * Horizontal velocity together with a walking direction should enqueue WALK animation.
     */
    @Test
    fun `horizontal movement enqueues walk animation`() {
        val animationCmp = with(world) { entity[AnimationComponent] }
        val inputCmp = with(world) { entity[InputComponent] }

        inputCmp.direction = WalkDirection.RIGHT

        world.update(0.016f)

        Assertions.assertEquals(
            AnimationType.WALK,
            animationCmp.nextAnimationType,
            "Entity with horizontal movement should enqueue WALK animation",
        )
    }

    /**
     * Setting the jump‑flag should enqueue JUMP animation irrespective of horizontal velocity.
     */
    @Test
    fun `jump flag enqueues jump animation`() {
        val animationCmp = with(world) { entity[AnimationComponent] }
        val inputCmp = with(world) { entity[InputComponent] }

        inputCmp.jumpJustPressed = true
        world.update(0.016f)

        Assertions.assertEquals(
            AnimationType.JUMP,
            animationCmp.nextAnimationType,
            "Entity that starts jumping should enqueue JUMP animation",
        )
    }
}
