package unitTests

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.Body
import com.badlogic.gdx.physics.box2d.Fixture
import com.badlogic.gdx.physics.box2d.RayCastCallback
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import com.github.quillraven.fleks.configureWorld
import io.bennyoe.components.ImageComponent
import io.bennyoe.components.IntentionComponent
import io.bennyoe.components.PhysicComponent
import io.bennyoe.components.PlayerComponent
import io.bennyoe.components.TransformComponent
import io.bennyoe.components.ai.BasicSensorsComponent
import io.bennyoe.components.ai.BasicSensorsHitComponent
import io.bennyoe.components.ai.LedgeSensorsComponent
import io.bennyoe.components.ai.LedgeSensorsHitComponent
import io.bennyoe.components.ai.SensorDef
import io.bennyoe.config.EntityCategory
import io.bennyoe.systems.ai.LedgeSensorsSystem
import io.bennyoe.systems.debug.DefaultDebugRenderService
import io.bennyoe.utility.EntityBodyData
import io.bennyoe.utility.SensorType
import io.mockk.every
import io.mockk.mockk
import ktx.math.vec2
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class LedgeSensorsSystemUnitTest {
    private lateinit var ecsWorld: World
    private lateinit var phyWorld: com.badlogic.gdx.physics.box2d.World
    private lateinit var enemy: Entity
    private lateinit var player: Entity
    private lateinit var bodyMock: Body

    @BeforeEach
    fun setup() {
        phyWorld = mockk(relaxed = true)
        bodyMock = mockk(relaxed = true)
        val stageMock = mockk<Stage>(relaxed = true)
        val transformCmp = mockk<TransformComponent>(relaxed = true)
        val imageMock: Image = mockk(relaxed = true)
        val imgCmp =
            ImageComponent(stageMock).also {
                it.image = imageMock
            }

        val debugRenderService = mockk<DefaultDebugRenderService>(relaxed = true)

        ecsWorld =
            configureWorld {
                injectables {
                    add("phyWorld", phyWorld)
                    add("debugRenderService", debugRenderService)
                }
                systems {
                    add(LedgeSensorsSystem())
                }
            }

        player =
            ecsWorld.entity {
                val pCmp = PhysicComponent()
                pCmp.body = bodyMock
                it += pCmp
                it += PlayerComponent()
            }

        enemy =
            ecsWorld.entity { entity ->
                val pCmp = PhysicComponent()
                pCmp.body = bodyMock
                entity += pCmp
                entity += IntentionComponent().apply { wantsToChase = true }
                entity += imgCmp
                entity += LedgeSensorsComponent()
                entity += LedgeSensorsHitComponent()
                entity +=
                    BasicSensorsComponent(
                        listOf(
                            SensorDef(
                                bodyAnchorPoint = vec2(1f, -0.9f),
                                rayLengthOffset = vec2(1.5f, 0f),
                                type = SensorType.WALL_SENSOR,
                                name = "minotaur_wall",
                                color = Color.BLUE,
                                hitFilter = {
                                    it.entityCategory == EntityCategory.GROUND ||
                                        it.entityCategory == EntityCategory.WORLD_BOUNDARY
                                },
                            ),
                        ),
                        7f,
                        transformCmp,
                        23f,
                    )
                entity += BasicSensorsHitComponent()
            }
    }

    @Test
    fun `ledge sensors add LedgeHitData`() {
        val ledgeFixture = mockk<Fixture>()
        val ledgeBody = mockk<Body>()
        val groundEntity = mockk<Entity>()
        every { ledgeFixture.body } returns ledgeBody
        every { ledgeBody.userData } returns EntityBodyData(entityCategory = EntityCategory.GROUND, entity = groundEntity)

        every { phyWorld.rayCast(any(), any(), any()) } answers {
            val cb = firstArg<RayCastCallback>()
            cb.reportRayFixture(ledgeFixture, Vector2.Zero, Vector2.Y, 0.5f)
        }

        ecsWorld.update(1f)

        val ledgeSensorsHitCmp = with(ecsWorld) { enemy[LedgeSensorsHitComponent] }
        assertTrue(ledgeSensorsHitCmp.upperLedgeHits.any { it.hit }, "Upper ledge sensor should detect ground")
        assertTrue(ledgeSensorsHitCmp.lowerLedgeHits.any { it.hit }, "Lower ledge sensor should detect ground")
    }
}
