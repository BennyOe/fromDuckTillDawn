package io.bennyoe.screens

import box2dLight.RayHandler
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ai.GdxAI
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.graphics.profiling.GLProfiler
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.github.bennyOe.core.Scene2dLightEngine
import com.github.quillraven.fleks.configureWorld
import io.bennyoe.Stages
import io.bennyoe.assets.MapAssets
import io.bennyoe.assets.TextureAssets
import io.bennyoe.components.GameStateComponent
import io.bennyoe.components.debug.DebugComponent
import io.bennyoe.config.GameConstants.ENABLE_DEBUG
import io.bennyoe.config.GameConstants.GRAVITY
import io.bennyoe.config.GameConstants.TIME_SCALE
import io.bennyoe.event.MapChangedEvent
import io.bennyoe.event.fire
import io.bennyoe.service.DefaultDebugRenderService
import io.bennyoe.systems.AnimationSystem
import io.bennyoe.systems.AttackSystem
import io.bennyoe.systems.BasicSensorsSystem
import io.bennyoe.systems.BehaviorTreeSystem
import io.bennyoe.systems.CameraSystem
import io.bennyoe.systems.CollisionSpawnSystem
import io.bennyoe.systems.DamageSystem
import io.bennyoe.systems.EntitySpawnSystem
import io.bennyoe.systems.ExpireSystem
import io.bennyoe.systems.GameStateSystem
import io.bennyoe.systems.InputSystem
import io.bennyoe.systems.JumpSystem
import io.bennyoe.systems.MoveSystem
import io.bennyoe.systems.PhysicsSystem
import io.bennyoe.systems.RenderSystem
import io.bennyoe.systems.StateSystem
import io.bennyoe.systems.UiRenderSystem
import io.bennyoe.systems.debug.BTBubbleSystem
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
    private val dawnAtlas = assets[TextureAssets.DAWN_ATLAS.descriptor]
    private val dawnNormalAtlas = assets[TextureAssets.DAWN_N_ATLAS.descriptor]
    private val mushroomAtlas = assets[TextureAssets.MUSHROOM_ATLAS.descriptor]
    private val tiledMap = assets[MapAssets.TEST_MAP.descriptor]
    private val stages = context.inject<Stages>()
    private val stage = stages.stage
    private val uiStage = stages.uiStage
    private val spriteBatch = context.inject<SpriteBatch>()
    private val phyWorld =
        createWorld(gravity = Vector2(0f, GRAVITY), true).apply {
            autoClearForces = false
        }
    private val rayHandler = RayHandler(phyWorld)
    private val lightEngine = Scene2dLightEngine(rayHandler, stage.camera as OrthographicCamera, spriteBatch, stage.viewport, stage)
    private val profiler by lazy { GLProfiler(Gdx.graphics) }
    private val entityWorld =
        configureWorld {
            injectables {
                add("phyWorld", phyWorld)
                add("dawnAtlas", dawnAtlas)
                add("dawnNormalAtlas", dawnNormalAtlas)
                add("mushroomAtlas", mushroomAtlas)
                add("stage", stage)
                add("uiStage", uiStage)
                add("shapeRenderer", ShapeRenderer())
                add("debugRenderService", DefaultDebugRenderService())
                add("spriteBatch", spriteBatch)
                add("profiler", profiler)
                add("lightEngine", lightEngine)
            }
            systems {
                add(AnimationSystem())
                add(EntitySpawnSystem())
                add(CollisionSpawnSystem())
                add(InputSystem())
                add(AttackSystem())
                add(GameStateSystem())
                add(DamageSystem())
                add(DamageTextSystem())
                add(JumpSystem())
                add(PhysicsSystem())
                add(BasicSensorsSystem())
                add(StateSystem())
                add(BehaviorTreeSystem())
                add(MoveSystem())
                add(CameraSystem())
                add(RenderSystem())
                if (ENABLE_DEBUG) add(DebugSystem())
                add(ExpireSystem())
                add(StateBubbleSystem())
                add(BTBubbleSystem())
                add(UiRenderSystem())
            }
        }

    override fun show() {
        profiler.enable()
        // add a gameState Entity to the screen
        entityWorld.entity {
            if (ENABLE_DEBUG) it += DebugComponent()
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
        GdxAI.getTimepiece().update(delta * TIME_SCALE)
        entityWorld.update(delta.coerceAtMost(0.25f) * TIME_SCALE)
    }

    override fun dispose() {
        dawnAtlas.dispose()
        dawnNormalAtlas.dispose()
        mushroomAtlas.dispose()
        entityWorld.dispose()
        tiledMap.disposeSafely()
    }

    override fun resize(
        width: Int,
        height: Int,
    ) {
        super.resize(width, height)
        lightEngine.resize(width, height)
    }

    companion object {
        private val logger = logger<GameScreen>()
    }
}
