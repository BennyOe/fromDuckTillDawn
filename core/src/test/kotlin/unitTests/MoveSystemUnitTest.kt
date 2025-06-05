package unitTests

import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import com.github.quillraven.fleks.configureWorld
import io.bennyoe.components.AnimationComponent
import io.bennyoe.components.ImageComponent
import io.bennyoe.components.InputComponent
import io.bennyoe.components.MoveComponent
import io.bennyoe.components.PhysicComponent
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

        val imageMock: Image = mockk(relaxed = true)
        val imgCmp =
            ImageComponent(stageMock).also {
                it.image = imageMock
            }
        world =
            configureWorld {
                systems {
                    add(MoveSystem())
                }
            }

        entity =
            world.entity {
                it += PhysicComponent()
                it += MoveComponent(maxSpeed = 10f)
                it += imgCmp
                it += InputComponent()
                it += AnimationComponent()
            }
    }

    @Test
    fun `entity moves right when input is RIGHT`() {
        val inputCmp = with(world) { entity[InputComponent] }
        inputCmp.walkRightPressed = true
        world.update(1f)

        val move = with(world) { entity[MoveComponent] }
        assertEquals(10f, move.moveVelocity)
    }

    @Test
    fun `entity velocity is 0 when standing still`() {
        with(world) { entity[InputComponent] }
        world.update(1f)
        val move = with(world) { entity[MoveComponent] }

        assertEquals(0f, move.moveVelocity)
    }

    @Test
    fun `entity moves left when input is LEFT`() {
        with(world) { entity[InputComponent].walkLeftPressed = true }
        world.update(1f)

        val move = with(world) { entity[MoveComponent] }
        assertEquals(-10f, move.moveVelocity)
    }

    @Test
    fun `entity moves at max speed regardless of delta time`() {
        // small Δt
        world.update(0.016f)
        val smallDtVelocity = with(world) { entity[MoveComponent].moveVelocity }

        // large Δt
        world.update(0.5f)
        val largeDtVelocity = with(world) { entity[MoveComponent].moveVelocity }

        assertEquals(
            smallDtVelocity,
            largeDtVelocity,
            "Velocity should be independent of the frame's delta time",
        )
    }

    @Test
    fun `entity reaches max speed after multiple small updates`() {
        val inputCmp = with(world) { entity[InputComponent] }
        inputCmp.walkRightPressed = true
        repeat(10) { world.update(0.016f) } // simulate ~10 frames @60 FPS
        val vel = with(world) { entity[MoveComponent].moveVelocity }
        assertEquals(10f, vel)
    }

    @Test
    fun `velocity is clamped to max speed`() {
        val inputCmp = with(world) { entity[InputComponent] }
        inputCmp.walkRightPressed = true
        with(world) { entity[MoveComponent].maxSpeed = 5f }
        world.update(5f) // simulate a big lag spike
        val vel = with(world) { entity[MoveComponent].moveVelocity }
        assertEquals(5f, vel)
    }

    @Test
    fun `entity changes direction correctly within one frame`() {
        world.update(0.016f) // first frame moving right

        // switch direction to left and update again
        with(world) { entity[InputComponent].walkLeftPressed = true }
        world.update(0.016f)

        val vel = with(world) { entity[MoveComponent].moveVelocity }
        assertEquals(-10f, vel)
    }
}
