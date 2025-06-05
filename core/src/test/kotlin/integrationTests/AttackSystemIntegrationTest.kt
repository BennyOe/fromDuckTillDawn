package integrationTests

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ai.fsm.DefaultStateMachine
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.Body
import com.badlogic.gdx.physics.box2d.BodyDef
import com.badlogic.gdx.physics.box2d.FixtureDef
import com.badlogic.gdx.physics.box2d.PolygonShape
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import com.github.quillraven.fleks.configureWorld
import io.bennyoe.components.AnimationComponent
import io.bennyoe.components.AttackComponent
import io.bennyoe.components.HealthComponent
import io.bennyoe.components.ImageComponent
import io.bennyoe.components.InputComponent
import io.bennyoe.components.JumpComponent
import io.bennyoe.components.MoveComponent
import io.bennyoe.components.PhysicComponent
import io.bennyoe.components.StateComponent
import io.bennyoe.config.EntityCategory
import io.bennyoe.service.DebugRenderService
import io.bennyoe.state.player.PlayerCheckAliveState
import io.bennyoe.state.player.PlayerFSM
import io.bennyoe.state.player.PlayerStateContext
import io.bennyoe.systems.AttackSystem
import io.bennyoe.utility.BodyData
import io.bennyoe.utility.FixtureData
import io.bennyoe.utility.FixtureType
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class AttackSystemIntegrationTest {
    private lateinit var world: World
    private lateinit var entity: Entity
    private lateinit var enemy: Entity
    private lateinit var phyWorld: com.badlogic.gdx.physics.box2d.World
    private lateinit var debugRenderService: DebugRenderService
    private lateinit var imgCmp: ImageComponent
    private lateinit var imageMock: Image

    @BeforeEach
    fun setup() {
        Gdx.app = mockk<Application>(relaxed = true)
        val mockAnimationCmp = mockk<AnimationComponent>(relaxed = true)
        val stageMock = mockk<Stage>(relaxed = true)
        phyWorld =
            com.badlogic.gdx.physics.box2d
                .World(Vector2(0f, -9.81f), true)
        debugRenderService = mockk<DebugRenderService>(relaxed = true)
        imageMock = mockk(relaxed = true)
        imgCmp =
            ImageComponent(stageMock).also {
                it.image = imageMock
            }

        world =
            configureWorld {
                injectables {
                    add("phyWorld", phyWorld)
                    add("debugRenderService", debugRenderService)
                }
                systems { add(AttackSystem()) }
            }

        val playerBody = mockk<Body>(relaxed = true)
        val enemyBody =
            phyWorld.createBody(
                BodyDef().apply {
                    type = BodyDef.BodyType.DynamicBody
                    position.set(1f, 0f)
                },
            )

        val playerPhysicCmp =
            PhysicComponent().apply {
                body = playerBody
                size.set(1f, 1f)
                categoryBits = EntityCategory.PLAYER.bit
            }

        val enemyPhysicCmp =
            PhysicComponent().apply {
                body = enemyBody
                size.set(1f, 1f)
                categoryBits = EntityCategory.ENEMY.bit
            }

        enemy =
            world.entity {
                it += mockAnimationCmp
                it += enemyPhysicCmp
                it += MoveComponent()
                it += HealthComponent()
                it += JumpComponent()
            }
        enemyBody.userData = BodyData(EntityCategory.ENEMY, enemy)

        // Add real fixture with HITBOX_SENSOR userData
        val shape = PolygonShape().apply { setAsBox(0.5f, 0.5f) }
        val fixtureDef =
            FixtureDef().apply {
                this.shape = shape
                isSensor = true
                filter.categoryBits = EntityCategory.ENEMY.bit
            }
        val fixture = enemyBody.createFixture(fixtureDef)
        fixture.userData = FixtureData(FixtureType.HITBOX_SENSOR)
        shape.dispose()

        entity =
            world.entity {
                it += mockAnimationCmp
                it += playerPhysicCmp
                it += AttackComponent()
                it += MoveComponent()
                it += HealthComponent()
                it += imgCmp
                it += InputComponent()
                it += JumpComponent()
                it +=
                    StateComponent(
                        world,
                        PlayerStateContext(it, world),
                        PlayerFSM.IDLE,
                        PlayerCheckAliveState,
                        ::DefaultStateMachine,
                    )
            }
    }

    @Test
    fun `if attack is executed damage to enemy is done`() {
        with(world) { entity[AttackComponent].applyAttack = true }
        val attackComponent = with(world) { entity[AttackComponent] }
        attackComponent.attackDelay = 0f

        world.update(0.061f)

        val damage = with(world) { entity[AttackComponent].damage }
        val actual = with(world) { enemy[HealthComponent].takenDamage }
        assertEquals(damage, actual, 1f)
    }

    @Test
    fun `if attack is executed NO damage to player is done`() {
        with(world) { entity[AttackComponent].applyAttack = true }

        // Simulate the effect of a successful hit by manually applying damage
        val playerAttackCmp = with(world) { entity[AttackComponent] }
        val enemyHealthCmp = with(world) { enemy[HealthComponent] }
        enemyHealthCmp.takeDamage(playerAttackCmp.damage)

        world.update(0.061f)

        val playerHealthCmp = with(world) { entity[HealthComponent] }
        assertEquals(0f, playerHealthCmp.takenDamage)
    }

    @Test
    fun `attack does not apply if applyAttack is false`() {
        with(world) { entity[AttackComponent].applyAttack = false }

        val enemyHealthCmp = with(world) { enemy[HealthComponent] }
        world.update(0.061f)

        assertEquals(0f, enemyHealthCmp.takenDamage)
    }

    @Test
    fun `attack does not hit entity with same categoryBits`() {
        with(world) {
            entity[AttackComponent].applyAttack = true
            val enemyPhysic = enemy[PhysicComponent]

            val fixture = enemyPhysic.body.fixtureList.firstOrNull()
            fixture?.filterData =
                fixture.filterData?.apply {
                    categoryBits = EntityCategory.PLAYER.bit
                }
        }

        world.update(0.061f)

        val enemyHealthCmp = with(world) { enemy[HealthComponent] }
        assertEquals(0f, enemyHealthCmp.takenDamage)
    }

    @Test
    fun `attack does not apply if fixture is not HITBOX_SENSOR`() {
        val enemyPhyCmp = with(world) { enemy[PhysicComponent] }
        val firstFixture = enemyPhyCmp.body.fixtureList.firstOrNull()
        firstFixture?.userData = BodyData(EntityCategory.ENEMY, enemy)

        with(world) { entity[AttackComponent].applyAttack = true }

        val enemyHealthCmp = with(world) { enemy[HealthComponent] }
        world.update(0.061f)
        assertEquals(0f, enemyHealthCmp.takenDamage)
    }
}
