package io.bennyoe.screens

import box2dLight.RayHandler
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ai.GdxAI
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.graphics.profiling.GLProfiler
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.github.quillraven.fleks.configureWorld
import io.bennyoe.Stages
import io.bennyoe.assets.MapAssets
import io.bennyoe.assets.TextureAssets
import io.bennyoe.assets.TextureAtlases
import io.bennyoe.components.CameraComponent
import io.bennyoe.components.GameStateComponent
import io.bennyoe.components.debug.DebugComponent
import io.bennyoe.config.EntityCategory
import io.bennyoe.config.GameConstants.ENABLE_DEBUG
import io.bennyoe.config.GameConstants.GRAVITY
import io.bennyoe.config.GameConstants.TIME_SCALE
import io.bennyoe.event.MapChangedEvent
import io.bennyoe.event.fire
import io.bennyoe.lightEngine.core.Scene2dLightEngine
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
import io.bennyoe.systems.LightSystem
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
import kotlin.experimental.and
import kotlin.experimental.inv

class GameScreen(
    context: Context,
) : AbstractScreen(context) {
    private val assets = context.inject<AssetStorage>()
    private val dawnAtlases =
        TextureAtlases(
            assets[TextureAssets.DAWN_ATLAS.descriptor],
            assets[TextureAssets.DAWN_N_ATLAS.descriptor],
            assets[TextureAssets.DAWN_S_ATLAS.descriptor],
        )
    private val mushroomAtlases =
        TextureAtlases(
            assets[TextureAssets.MUSHROOM_ATLAS.descriptor],
            assets[TextureAssets.MUSHROOM_N_ATLAS.descriptor],
            assets[TextureAssets.MUSHROOM_S_ATLAS.descriptor],
        )
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
    private val lightEngine =
        Scene2dLightEngine(
            rayHandler = rayHandler,
            cam = stage.camera as OrthographicCamera,
            batch = spriteBatch,
            viewport = stage.viewport,
            stage = stage,
            entityCategory = EntityCategory.LIGHT.bit,
            entityMask = (EntityCategory.ALL.bit and EntityCategory.WORLD_BOUNDARY.bit.inv()),
            lightActivationRadius = 18f,
        )
    private val profiler by lazy { GLProfiler(Gdx.graphics) }
    private val entityWorld =
        configureWorld {
            injectables {
                add("phyWorld", phyWorld)
                add("dawnAtlases", dawnAtlases)
                add("mushroomAtlases", mushroomAtlases)
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
                add(LightSystem())
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
            it += CameraComponent()
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
        dawnAtlases.diffuseAtlas.dispose()
        dawnAtlases.normalAtlas?.dispose()
        dawnAtlases.specularAtlas?.dispose()
        mushroomAtlases.diffuseAtlas.dispose()
        mushroomAtlases.normalAtlas?.dispose()
        mushroomAtlases.specularAtlas?.dispose()
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
