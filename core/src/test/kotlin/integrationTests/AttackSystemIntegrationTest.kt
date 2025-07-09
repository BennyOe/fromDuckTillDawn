package integrationTests

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.Body
import com.badlogic.gdx.physics.box2d.BodyDef
import com.badlogic.gdx.physics.box2d.FixtureDef
import com.badlogic.gdx.physics.box2d.PolygonShape
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.github.bennyOe.gdxNormalLight.core.GameLight
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import com.github.quillraven.fleks.configureWorld
import io.bennyoe.components.AnimationComponent
import io.bennyoe.components.AttackComponent
import io.bennyoe.components.HealthComponent
import io.bennyoe.components.ImageComponent
import io.bennyoe.components.InputComponent
import io.bennyoe.components.IntentionComponent
import io.bennyoe.components.JumpComponent
import io.bennyoe.components.LightComponent
import io.bennyoe.components.MoveComponent
import io.bennyoe.components.PhysicComponent
import io.bennyoe.components.StateComponent
import io.bennyoe.config.EntityCategory
import io.bennyoe.service.DebugRenderService
import io.bennyoe.state.mushroom.MushroomCheckAliveState
import io.bennyoe.state.mushroom.MushroomFSM
import io.bennyoe.state.mushroom.MushroomStateContext
import io.bennyoe.state.player.PlayerCheckAliveState
import io.bennyoe.state.player.PlayerFSM
import io.bennyoe.state.player.PlayerStateContext
import io.bennyoe.systems.AttackSystem
import io.bennyoe.systems.InputSystem
import io.bennyoe.utility.BodyData
import io.bennyoe.utility.FixtureData
import io.bennyoe.utility.SensorType
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class AttackSystemIntegrationTest {
    private lateinit var world: World
    private lateinit var playerEntity: Entity
    private lateinit var enemyEntity: Entity
    private lateinit var phyWorld: com.badlogic.gdx.physics.box2d.World

    @BeforeEach
    fun setup() {
        // --------------- Setup World ----------------
        Gdx.app = mockk<Application>(relaxed = true)
        val stageMock = mockk<Stage>(relaxed = true)
        val gameLight = mockk<GameLight>(relaxed = true)
        val debugRenderServiceMock = mockk<DebugRenderService>(relaxed = true)
        val animationCmpMock = mockk<AnimationComponent>(relaxed = true)

        phyWorld =
            com.badlogic.gdx.physics.box2d
                .World(Vector2(0f, -9.81f), true)
        world =
            configureWorld {
                injectables {
                    add("phyWorld", phyWorld)
                    add("debugRenderService", debugRenderServiceMock)
                }
                systems {
                    add(AttackSystem())
                    add(InputSystem())
                }
            }

        // --------------- Setup Player ----------------
        val playerBody = mockk<Body>(relaxed = true)
        val imageMock = mockk<Image>(relaxed = true)

        val playerImageCmp =
            ImageComponent(stageMock).also {
                it.image = imageMock
            }

        val playerPhysicCmp =
            PhysicComponent().apply {
                body = playerBody
                size.set(1f, 1f)
                categoryBits = EntityCategory.PLAYER.bit
            }
        playerEntity =
            world.entity {
                it += animationCmpMock
                it += playerPhysicCmp
                it += AttackComponent()
                it += MoveComponent()
                it += IntentionComponent()
                it += HealthComponent()
                it += LightComponent(gameLight)
                it += playerImageCmp
                it += InputComponent()
                it += JumpComponent()
                it +=
                    StateComponent(
                        world,
                        PlayerStateContext(it, world),
                        PlayerFSM.IDLE,
                        PlayerCheckAliveState,
                    )
            }

        // --------------- Setup Enemy ----------------
        val enemyBody =
            phyWorld.createBody(
                BodyDef().apply {
                    type = BodyDef.BodyType.DynamicBody
                    position.set(1f, 0f)
                },
            )

        val enemyPhysicCmp =
            PhysicComponent().apply {
                body = enemyBody
                size.set(1f, 1f)
                categoryBits = EntityCategory.ENEMY.bit
            }

        enemyEntity =
            world.entity {
                it += animationCmpMock
                it += enemyPhysicCmp
                it += MoveComponent()
                it += IntentionComponent()
                it += HealthComponent()
                it += LightComponent(gameLight)
                it += JumpComponent()
                it +=
                    StateComponent(
                        world,
                        MushroomStateContext(it, world),
                        MushroomFSM.IDLE,
                        MushroomCheckAliveState,
                    )
            }

        enemyBody.userData = BodyData(EntityCategory.ENEMY, enemyEntity)

        // --------------- Setup Fixture ----------------
        val shape = PolygonShape().apply { setAsBox(0.5f, 0.5f) }
        val fixtureDef =
            FixtureDef().apply {
                this.shape = shape
                isSensor = true
                filter.categoryBits = EntityCategory.ENEMY.bit
            }
        val fixture = enemyBody.createFixture(fixtureDef)
        fixture.userData = FixtureData(SensorType.HITBOX_SENSOR)
        shape.dispose()
    }

    @Test
    fun `if attack is executed damage to enemy is done`() {
        val attackCmp = with(world) { playerEntity[AttackComponent] }
        val inputCmp = with(world) { playerEntity[InputComponent] }

        val healthCmp = with(world) { enemyEntity[HealthComponent] }

        @Suppress("UNCHECKED_CAST")
        val playerStateCmp = with(world) { playerEntity[StateComponent] as StateComponent<PlayerStateContext, PlayerFSM> }

        @Suppress("UNCHECKED_CAST")
        val enemyStateCmp = with(world) { enemyEntity[StateComponent] as StateComponent<MushroomStateContext, MushroomFSM> }

        inputCmp.attackJustPressed = true
        attackCmp.attackDelay = 0f

        world.update(0.061f)
        playerStateCmp.stateMachine.update()
        assertEquals(PlayerFSM.ATTACK_1, playerStateCmp.stateMachine.currentState)

        world.update(0.061f)
        enemyStateCmp.stateMachine.update()

        val damage = attackCmp.damage
        assertEquals(damage, healthCmp.takenDamage, 1f)
    }

    @Test
    fun `if attack is executed NO damage to player is done`() {
        val attackCmp = with(world) { playerEntity[AttackComponent] }
        val inputCmp = with(world) { playerEntity[InputComponent] }

        val enemyHealthCmp = with(world) { enemyEntity[HealthComponent] }
        val playerHealthCmp = with(world) { playerEntity[HealthComponent] }

        @Suppress("UNCHECKED_CAST")
        val playerStateCmp = with(world) { playerEntity[StateComponent] as StateComponent<PlayerStateContext, PlayerFSM> }

        @Suppress("UNCHECKED_CAST")
        val enemyStateCmp = with(world) { enemyEntity[StateComponent] as StateComponent<MushroomStateContext, MushroomFSM> }

        inputCmp.attackJustPressed = true
        attackCmp.attackDelay = 0f

        world.update(0.061f)
        playerStateCmp.stateMachine.update()
        assertEquals(PlayerFSM.ATTACK_1, playerStateCmp.stateMachine.currentState)

        world.update(0.061f)
        enemyStateCmp.stateMachine.update()

        val damage = attackCmp.damage
        assertEquals(damage, enemyHealthCmp.takenDamage, 1f)
        assertEquals(0f, playerHealthCmp.takenDamage)
    }

    @Test
    fun `attack does not apply if no attack is triggered`() {
        val attackCmp = with(world) { playerEntity[AttackComponent] }
        val inputCmp = with(world) { playerEntity[InputComponent] }

        @Suppress("UNCHECKED_CAST")
        val playerStateCmp = with(world) { playerEntity[StateComponent] as StateComponent<PlayerStateContext, PlayerFSM> }

        inputCmp.attackJustPressed = false
        attackCmp.attackDelay = 0f

        world.update(0.061f)
        playerStateCmp.stateMachine.update()
        assertEquals(PlayerFSM.IDLE, playerStateCmp.stateMachine.currentState)
    }

    @Test
    fun `attack does not hit entity with same categoryBits`() {
        val attackCmp = with(world) { playerEntity[AttackComponent] }
        val inputCmp = with(world) { playerEntity[InputComponent] }

        val enemyPhysicCmp = with(world) { enemyEntity[PhysicComponent] }
        val healthCmp = with(world) { enemyEntity[HealthComponent] }

        @Suppress("UNCHECKED_CAST")
        val playerStateCmp = with(world) { playerEntity[StateComponent] as StateComponent<PlayerStateContext, PlayerFSM> }

        @Suppress("UNCHECKED_CAST")
        val enemyStateCmp = with(world) { enemyEntity[StateComponent] as StateComponent<MushroomStateContext, MushroomFSM> }

        val fixture = enemyPhysicCmp.body.fixtureList.firstOrNull()
        fixture?.filterData =
            fixture.filterData?.apply {
                categoryBits = EntityCategory.PLAYER.bit
            }

        inputCmp.attackJustPressed = true
        attackCmp.attackDelay = 0f

        world.update(0.061f)
        playerStateCmp.stateMachine.update()
        assertEquals(PlayerFSM.ATTACK_1, playerStateCmp.stateMachine.currentState)

        world.update(0.061f)
        enemyStateCmp.stateMachine.update()

        assertEquals(0f, healthCmp.takenDamage)
    }

    @Test
    fun `attack does not apply if fixture is not HITBOX_SENSOR`() {
        val attackCmp = with(world) { playerEntity[AttackComponent] }
        val inputCmp = with(world) { playerEntity[InputComponent] }

        val enemyPhysicCmp = with(world) { enemyEntity[PhysicComponent] }
        val healthCmp = with(world) { enemyEntity[HealthComponent] }

        @Suppress("UNCHECKED_CAST")
        val playerStateCmp = with(world) { playerEntity[StateComponent] as StateComponent<PlayerStateContext, PlayerFSM> }

        @Suppress("UNCHECKED_CAST")
        val enemyStateCmp = with(world) { enemyEntity[StateComponent] as StateComponent<MushroomStateContext, MushroomFSM> }

        val firstFixture = enemyPhysicCmp.body.fixtureList.firstOrNull()
        firstFixture?.userData = BodyData(EntityCategory.ENEMY, enemyEntity)

        inputCmp.attackJustPressed = true
        attackCmp.attackDelay = 0f

        world.update(0.061f)
        playerStateCmp.stateMachine.update()
        assertEquals(PlayerFSM.ATTACK_1, playerStateCmp.stateMachine.currentState)

        world.update(0.061f)
        enemyStateCmp.stateMachine.update()

        assertEquals(0f, healthCmp.takenDamage)
    }
}
