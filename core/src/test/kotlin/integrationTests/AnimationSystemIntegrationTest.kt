package integrationTests

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.physics.box2d.Body
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import com.github.quillraven.fleks.configureWorld
import io.bennyoe.assets.TextureAtlases
import io.bennyoe.components.AnimationComponent
import io.bennyoe.components.AnimationType
import io.bennyoe.components.AttackComponent
import io.bennyoe.components.HasGroundContact
import io.bennyoe.components.HealthComponent
import io.bennyoe.components.ImageComponent
import io.bennyoe.components.InputComponent
import io.bennyoe.components.IntentionComponent
import io.bennyoe.components.JumpComponent
import io.bennyoe.components.LightComponent
import io.bennyoe.components.MoveComponent
import io.bennyoe.components.PhysicComponent
import io.bennyoe.components.StateComponent
import io.bennyoe.lightEngine.core.GameLight
import io.bennyoe.state.player.PlayerCheckAliveState
import io.bennyoe.state.player.PlayerFSM
import io.bennyoe.state.player.PlayerStateContext
import io.bennyoe.systems.AnimationSystem
import io.bennyoe.systems.InputSystem
import io.bennyoe.systems.MoveSystem
import io.bennyoe.systems.StateSystem
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
        val gameLight = mockk<GameLight>(relaxed = true)
        // Set up the TextureAtlas mock to return a non-empty regions array
        val atlasRegionMock = mockk<TextureAtlas.AtlasRegion>(relaxed = true)
        val regions =
            com.badlogic.gdx.utils
                .Array<TextureAtlas.AtlasRegion>()
        regions.add(atlasRegionMock)
        io.mockk.every { atlasMock.findRegions(any()) } returns regions
        val stageMock = mockk<Stage>(relaxed = true)
        val bodyMock = mockk<Body>(relaxed = true)
        val regionMock = mockk<TextureRegion>(relaxed = true)
        val drawable = TextureRegionDrawable(regionMock)
        val animation =
            Animation(
                0.1f,
                gdxArrayOf(drawable),
                Animation.PlayMode.LOOP,
            )
        val animationCmp =
            AnimationComponent().apply {
                this.animation = animation
            }

        val imageMock: Image = mockk(relaxed = true)
        val imgCmp =
            ImageComponent(stageMock).also {
                it.image = imageMock
            }

        world =
            configureWorld {
                injectables {
                    add("dawnAtlases", TextureAtlases(atlasMock, atlasMock, atlasMock))
                    add("mushroomAtlases", TextureAtlases(atlasMock, atlasMock, atlasMock))
                }
                systems {
                    add(MoveSystem())
                    add(StateSystem())
                    add(InputSystem())
                    add(AnimationSystem())
                }
            }

        entity =
            world.entity {
                it += AttackComponent()
                it += MoveComponent()
                it += PhysicComponent().apply { body = bodyMock }
                it += HealthComponent()
                it += IntentionComponent()
                it += imgCmp
                it += animationCmp
                it += LightComponent(gameLight)
                it += JumpComponent()
                it += HasGroundContact
                it += InputComponent()
                it +=
                    StateComponent(
                        world,
                        PlayerStateContext(it, world),
                        PlayerFSM.IDLE,
                        PlayerCheckAliveState,
                    )
            }
    }

    @Test
    fun `no walk direction sets animation type to IDLE`() {
        val animationCmp = with(world) { entity[AnimationComponent] }
        val inputCmp = with(world) { entity[InputComponent] }

        @Suppress("UNCHECKED_CAST")
        val stateCmp = with(world) { entity[StateComponent] as StateComponent<PlayerStateContext, PlayerFSM> }

        inputCmp.walkLeftJustPressed = true
        world.update(1f)
        stateCmp.stateMachine.update()

        inputCmp.walkLeftJustPressed = false
        world.update(1f)
        stateCmp.stateMachine.update()

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

        @Suppress("UNCHECKED_CAST")
        val stateCmp = with(world) { entity[StateComponent] as StateComponent<PlayerStateContext, PlayerFSM> }

        inputCmp.walkRightJustPressed = true
        world.update(1f)
        stateCmp.stateMachine.update()

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

        @Suppress("UNCHECKED_CAST")
        val stateCmp = with(world) { entity[StateComponent] as StateComponent<PlayerStateContext, PlayerFSM> }

        inputCmp.jumpJustPressed = true
        with(world) { entity.configure { it += HasGroundContact } }

        world.update(0.016f)
        stateCmp.stateMachine.update()

        Assertions.assertEquals(
            AnimationType.JUMP,
            animationCmp.nextAnimationType,
            "Entity that starts jumping should enqueue JUMP animation",
        )
    }
}
