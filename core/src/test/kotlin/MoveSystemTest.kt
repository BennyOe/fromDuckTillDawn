import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import com.github.quillraven.fleks.configureWorld
import io.bennyoe.components.AnimationComponent
import io.bennyoe.components.InputComponent
import io.bennyoe.components.MoveComponent
import io.bennyoe.components.PhysicComponent
import io.bennyoe.components.WalkDirection
import io.bennyoe.systems.MoveSystem
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class MoveSystemTest {
    private lateinit var world: World
    private lateinit var entity: Entity

    @BeforeEach
    fun setup() {
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
                it += InputComponent(direction = WalkDirection.RIGHT)
                it += AnimationComponent()
            }
    }

    @Test
    fun `entity moves right when input is RIGHT`() {
        world.update(1f)

        val move = with(world) { entity[MoveComponent] }
        assertEquals(10f, move.moveVelocity)
    }

    @Test
    fun `entity velocity is 0 when standing still`() {
        with(world) { entity[InputComponent].direction = WalkDirection.NONE }
        world.update(1f)

        val move = with(world) { entity[MoveComponent] }
        assertEquals(0f, move.moveVelocity)
    }

    @Test
    fun `entity moves left when input is LEFT`() {
        with(world) { entity[InputComponent].direction = WalkDirection.LEFT }
        world.update(1f)

        val move = with(world) { entity[MoveComponent] }
        assertEquals(-10f, move.moveVelocity)
    }
}
