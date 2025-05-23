package io.bennyoe.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.graphics.profiling.GLProfiler
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.github.quillraven.fleks.configureWorld
import io.bennyoe.Stages
import io.bennyoe.assets.MapAssets
import io.bennyoe.assets.TextureAssets
import io.bennyoe.components.GameStateComponent
import io.bennyoe.components.debug.DebugComponent
import io.bennyoe.config.GameConstants.GRAVITY
import io.bennyoe.event.MapChangedEvent
import io.bennyoe.event.fire
import io.bennyoe.service.DebugRenderService
import io.bennyoe.systems.AnimationSystem
import io.bennyoe.systems.AttackSystem
import io.bennyoe.systems.CameraSystem
import io.bennyoe.systems.CollisionSpawnSystem
import io.bennyoe.systems.DamageSystem
import io.bennyoe.systems.DeadSystem
import io.bennyoe.systems.EntitySpawnSystem
import io.bennyoe.systems.JumpSystem
import io.bennyoe.systems.MoveSystem
import io.bennyoe.systems.PhysicsSystem
import io.bennyoe.systems.RenderSystem
import io.bennyoe.systems.StateSystem
import io.bennyoe.systems.UiRenderSystem
import io.bennyoe.systems.debug.DamageTextSystem
import io.bennyoe.systems.debug.DebugSystem
import io.bennyoe.systems.debug.StateBubbleSystem
import ktx.assets.async.AssetStorage
import ktx.assets.disposeSafely
import ktx.box2d.createWorld
import ktx.inject.Context
import ktx.log.logger

class GameScreen(
    context: Context,
) : AbstractScreen(context) {
    private val assets = context.inject<AssetStorage>()
    private val textureAtlas = assets[TextureAssets.PLAYER_ATLAS.descriptor]
    private val tiledMap = assets[MapAssets.TEST_MAP.descriptor]
    private val stages = context.inject<Stages>()
    private val stage = stages.stage
    private val uiStage = stages.uiStage
    private val spriteBatch = context.inject<SpriteBatch>()
    private val phyWorld =
        createWorld(gravity = Vector2(0f, GRAVITY), true).apply {
            autoClearForces = false
        }
    private val profiler by lazy { GLProfiler(Gdx.graphics) }
    private val entityWorld =
        configureWorld {
            injectables {
                add("phyWorld", phyWorld)
                add(textureAtlas)
                add("stage", stage)
                add("uiStage", uiStage)
                add("shapeRenderer", ShapeRenderer())
                add("debugRenderService", DebugRenderService())
                add("spriteBatch", spriteBatch)
                add("profiler", profiler)
            }
            systems {
                add(AnimationSystem())
                add(EntitySpawnSystem())
                add(CollisionSpawnSystem())
                add(AttackSystem())
                add(DamageSystem())
                add(DamageTextSystem())
                add(JumpSystem())
                add(PhysicsSystem())
                add(StateSystem())
                add(MoveSystem())
                add(CameraSystem())
                add(RenderSystem())
                add(DebugSystem())
                add(StateBubbleSystem())
                add(UiRenderSystem())
                add(DeadSystem())
            }
        }

    override fun show() {
        profiler.enable()
        // add a gameState Entity to the screen
        entityWorld.entity {
            it += DebugComponent()
            it += GameStateComponent()
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
        profiler.reset()
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
