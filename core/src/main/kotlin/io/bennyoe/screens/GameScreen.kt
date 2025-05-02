package io.bennyoe.screens

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.github.quillraven.fleks.configureWorld
import io.bennyoe.GameConstants.GRAVITY
import io.bennyoe.Stages
import io.bennyoe.assets.MapAssets
import io.bennyoe.assets.TextureAssets
import io.bennyoe.components.DebugComponent
import io.bennyoe.event.MapChangedEvent
import io.bennyoe.event.fire
import io.bennyoe.systems.AiSystem
import io.bennyoe.systems.AnimationSystem
import io.bennyoe.systems.CameraSystem
import io.bennyoe.systems.CollisionSpawnSystem
import io.bennyoe.systems.DebugSystem
import io.bennyoe.systems.EntitySpawnSystem
import io.bennyoe.systems.JumpSystem
import io.bennyoe.systems.MoveSystem
import io.bennyoe.systems.PhysicsSystem
import io.bennyoe.systems.RenderSystem
import io.bennyoe.systems.StateBubbleSystem
import io.bennyoe.systems.UiRenderSystem
import ktx.app.KtxScreen
import ktx.assets.async.AssetStorage
import ktx.assets.disposeSafely
import ktx.box2d.createWorld
import ktx.inject.Context
import ktx.log.logger

class GameScreen(
    context: Context,
) : KtxScreen {
    private val assets = context.inject<AssetStorage>()
    private val textureAtlas = assets[TextureAssets.PLAYER_ATLAS.descriptor]
    private val tiledMap = assets[MapAssets.TEST_MAP.descriptor]
    private val stages = context.inject<Stages>()
    private val stage = stages.stage
    private val uiStage = stages.uiStage
    private val phyWorld =
        createWorld(gravity = Vector2(0f, GRAVITY), true).apply {
            autoClearForces = false
        }
    private val entityWorld =
        configureWorld {
            injectables {
                add("phyWorld", phyWorld)
                add(textureAtlas)
                add("stage", stage)
                add("uiStage", uiStage)
            }
            systems {
                add(AnimationSystem())
                add(EntitySpawnSystem())
                add(CollisionSpawnSystem())
                add(PhysicsSystem())
                add(AiSystem())
                add(MoveSystem())
                add(CameraSystem())
                add(StateBubbleSystem())
                add(JumpSystem())
                add(RenderSystem())
                add(UiRenderSystem())
                add(DebugSystem())
            }
        }

    override fun show() {
        // add a gameState Entity to the screen
        val gameStateEntity =
            entityWorld.entity {
                it += DebugComponent()
            }

        // this adds all EventListenerSystems also to Scene2D
        entityWorld.systems.forEach { system ->
            if (system is EventListener) {
                stage.addListener(system)
            }
        }

        stage.fire(MapChangedEvent(tiledMap)) // mapChangeEvent gets fired

        super.show()
    }

    override fun render(delta: Float) {
        entityWorld.update(delta.coerceAtMost(0.25f))
    }

    override fun dispose() {
        textureAtlas.dispose()
        entityWorld.dispose()
        tiledMap.disposeSafely()
    }

    companion object {
        private val logger = logger<GameScreen>()
    }
}
