package integrationTests

import com.badlogic.gdx.physics.box2d.Body
import io.bennyoe.components.AnimationComponent
import io.bennyoe.components.AnimationType
import io.bennyoe.components.HasGroundContact
import io.bennyoe.components.InputComponent
import io.bennyoe.components.StateComponent
import io.bennyoe.state.player.PlayerFSM
import io.bennyoe.state.player.PlayerStateContext
import io.mockk.mockk
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AnimationSystemIntegrationTest : AbstractIntegrationTest() {
    @BeforeEach
    fun setup() {
        setupApp()
        val atlas = setupMockedAtlas()
        val (stage, animation) = setupStageAndDrawable()
        val body = mockk<Body>(relaxed = true)

        world = setupWorldWithSystems(atlas)
        playerEntity = createPlayerEntity(stage, animation, body)
    }

    @Test
    fun `no walk direction sets animation type to IDLE`() {
        val animationCmp = with(world) { playerEntity[AnimationComponent] }
        val inputCmp = with(world) { playerEntity[InputComponent] }

        @Suppress("UNCHECKED_CAST")
        val stateCmp = with(world) { playerEntity[StateComponent] as StateComponent<PlayerStateContext, PlayerFSM> }

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
        val animationCmp = with(world) { playerEntity[AnimationComponent] }
        val inputCmp = with(world) { playerEntity[InputComponent] }

        @Suppress("UNCHECKED_CAST")
        val stateCmp = with(world) { playerEntity[StateComponent] as StateComponent<PlayerStateContext, PlayerFSM> }

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
        val animationCmp = with(world) { playerEntity[AnimationComponent] }
        val inputCmp = with(world) { playerEntity[InputComponent] }

        @Suppress("UNCHECKED_CAST")
        val stateCmp = with(world) { playerEntity[StateComponent] as StateComponent<PlayerStateContext, PlayerFSM> }

        inputCmp.jumpJustPressed = true
        with(world) { playerEntity.configure { it += HasGroundContact } }

        world.update(0.016f)
        stateCmp.stateMachine.update()

        Assertions.assertEquals(
            AnimationType.JUMP,
            animationCmp.nextAnimationType,
            "Entity that starts jumping should enqueue JUMP animation",
        )
    }
}
