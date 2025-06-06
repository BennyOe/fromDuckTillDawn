package integrationTests

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ai.fsm.DefaultStateMachine
import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.physics.box2d.Body
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Array
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import com.github.quillraven.fleks.configureWorld
import io.bennyoe.components.AnimationComponent
import io.bennyoe.components.AttackComponent
import io.bennyoe.components.HasGroundContact
import io.bennyoe.components.HealthComponent
import io.bennyoe.components.ImageComponent
import io.bennyoe.components.InputComponent
import io.bennyoe.components.IntentionComponent
import io.bennyoe.components.JumpComponent
import io.bennyoe.components.MoveComponent
import io.bennyoe.components.PhysicComponent
import io.bennyoe.components.StateComponent
import io.bennyoe.state.player.PlayerCheckAliveState
import io.bennyoe.state.player.PlayerFSM
import io.bennyoe.state.player.PlayerStateContext
import io.bennyoe.systems.AnimationSystem
import io.bennyoe.systems.InputSystem
import io.bennyoe.systems.MoveSystem
import io.bennyoe.systems.StateSystem
import io.mockk.every
import io.mockk.mockk
import ktx.collections.gdxArrayOf

abstract class AbstractIntegrationTest {
    protected lateinit var world: World
    protected lateinit var playerEntity: Entity
    protected lateinit var enemyEntity: Entity
    protected lateinit var phyWorld: com.badlogic.gdx.physics.box2d.World

    protected fun setupApp() {
        Gdx.app = mockk<Application>(relaxed = true)
    }

    protected fun setupMockedAtlas(): TextureAtlas {
        val atlasMock = mockk<TextureAtlas>(relaxed = true)
        val regionMock = mockk<TextureAtlas.AtlasRegion>(relaxed = true)
        val regions = Array<TextureAtlas.AtlasRegion>()
        regions.add(regionMock)
        every { atlasMock.findRegions(any()) } returns regions
        return atlasMock
    }

    protected fun setupStageAndDrawable(): Pair<Stage, Animation<TextureRegionDrawable>> {
        val stageMock = mockk<Stage>(relaxed = true)
        val regionMock = mockk<TextureRegion>(relaxed = true)
        val drawable = TextureRegionDrawable(regionMock)
        val animation = Animation(0.1f, gdxArrayOf(drawable), Animation.PlayMode.LOOP)
        return stageMock to animation
    }

    protected fun setupWorldWithSystems(atlas: TextureAtlas): World =
        configureWorld {
            injectables {
                add(atlas)
            }
            systems {
                add(MoveSystem())
                add(StateSystem())
                add(InputSystem())
                add(AnimationSystem())
            }
        }

    protected fun createPlayerEntity(
        stage: Stage,
        animation: Animation<TextureRegionDrawable>,
        bodyMock: Body,
    ): Entity {
        val animCmp = AnimationComponent().apply { this.animation = animation }
        val imgCmp = ImageComponent(stage).also { it.image = mockk(relaxed = true) }

        return world.entity {
            it += AttackComponent()
            it += MoveComponent()
            it += PhysicComponent().apply { body = bodyMock }
            it += HealthComponent()
            it += IntentionComponent()
            it += imgCmp
            it += animCmp
            it += JumpComponent()
            it += HasGroundContact
            it += InputComponent()
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
}
