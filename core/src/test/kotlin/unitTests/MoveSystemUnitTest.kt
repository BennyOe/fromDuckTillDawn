package unitTests

import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.github.bennyOe.gdxNormalLight.core.GameLight
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import com.github.quillraven.fleks.configureWorld
import io.bennyoe.components.AnimationComponent
import io.bennyoe.components.ImageComponent
import io.bennyoe.components.InputComponent
import io.bennyoe.components.IntentionComponent
import io.bennyoe.components.LightComponent
import io.bennyoe.components.MoveComponent
import io.bennyoe.components.PhysicComponent
import io.bennyoe.systems.InputSystem
import io.bennyoe.systems.MoveSystem
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class MoveSystemUnitTest {
    private lateinit var world: World
    private lateinit var entity: Entity

    @BeforeEach
    fun setup() {
        val stageMock = mockk<Stage>(relaxed = true)

        val gameLight = mockk<GameLight>(relaxed = true)
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
                }
            }

        entity =
            world.entity {
                it += PhysicComponent()
                it += IntentionComponent()
                it += MoveComponent(maxSpeed = 10f)
                it += LightComponent(gameLight)
                it += imgCmp
                it += InputComponent()
                it += AnimationComponent()
            }
    }

    @Test
    fun `entity moves right when input is RIGHT`() {
        val inputCmp = with(world) { entity[InputComponent] }
        val moveCmp = with(world) { entity[MoveComponent] }

        inputCmp.walkRightJustPressed = true
        world.update(1f)

        assertEquals(10f, moveCmp.moveVelocity)
    }

    @Test
    fun `entity velocity is 0 when standing still`() {
        val moveCmp = with(world) { entity[MoveComponent] }

        world.update(1f)

        assertEquals(0f, moveCmp.moveVelocity)
    }

    @Test
    fun `entity moves left when input is LEFT`() {
        val inputCmp = with(world) { entity[InputComponent] }
        val moveCmp = with(world) { entity[MoveComponent] }

        inputCmp.walkLeftJustPressed = true
        world.update(1f)

        assertEquals(-10f, moveCmp.moveVelocity)
    }

    @Test
    fun `entity moves at max speed regardless of delta time`() {
        val moveCmp = with(world) { entity[MoveComponent] }

        // small Δt
        world.update(0.016f)
        val smallDtVelocity = moveCmp.moveVelocity

        // large Δt
        world.update(0.5f)
        val largeDtVelocity = moveCmp.moveVelocity

        assertEquals(
            smallDtVelocity,
            largeDtVelocity,
            "Velocity should be independent of the frame's delta time",
        )
    }

    @Test
    fun `entity reaches max speed after multiple small updates`() {
        val inputCmp = with(world) { entity[InputComponent] }
        val moveCmp = with(world) { entity[MoveComponent] }

        inputCmp.walkRightJustPressed = true
        repeat(10) { world.update(0.016f) }
        val vel = moveCmp.moveVelocity
        assertEquals(10f, vel)
    }

    @Test
    fun `velocity is clamped to max speed`() {
        val inputCmp = with(world) { entity[InputComponent] }
        val moveCmp = with(world) { entity[MoveComponent] }

        inputCmp.walkRightJustPressed = true
        moveCmp.maxSpeed = 5f

        world.update(5f) // simulate a big lag spike
        val vel = moveCmp.moveVelocity
        assertEquals(5f, vel)
    }

    @Test
    fun `entity changes direction correctly within one frame`() {
        val inputCmp = with(world) { entity[InputComponent] }
        val moveCmp = with(world) { entity[MoveComponent] }

        inputCmp.walkRightJustPressed = true
        world.update(0.016f) // first frame moving right

        // switch direction to left and update again
        inputCmp.walkRightJustPressed = false
        inputCmp.walkLeftJustPressed = true
        world.update(0.016f)

        val vel = moveCmp.moveVelocity
        assertEquals(-10f, vel)
    }
}
