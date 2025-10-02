package io.bennyoe.screens

import box2dLight.RayHandler
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.ai.GdxAI
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.graphics.glutils.GLFrameBuffer
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.graphics.profiling.GLProfiler
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.badlogic.gdx.utils.Align
import com.github.quillraven.fleks.configureWorld
import de.pottgames.tuningfork.Audio
import io.bennyoe.PlayerInputProcessor
import io.bennyoe.Stages
import io.bennyoe.assets.MapAssets
import io.bennyoe.assets.TextureAssets
import io.bennyoe.assets.TextureAtlases
import io.bennyoe.components.CameraComponent
import io.bennyoe.components.GameStateComponent
import io.bennyoe.components.TimeScaleComponent
import io.bennyoe.components.debug.DebugComponent
import io.bennyoe.config.EntityCategory
import io.bennyoe.config.GameConstants.ENABLE_DEBUG
import io.bennyoe.config.GameConstants.GRAVITY
import io.bennyoe.config.GameConstants.MAX_FPS
import io.bennyoe.config.GameConstants.TIME_SCALE
import io.bennyoe.config.GameConstants.VSYNC
import io.bennyoe.event.MapChangedEvent
import io.bennyoe.event.fire
import io.bennyoe.lightEngine.core.Scene2dLightEngine
import io.bennyoe.systems.AnimationSystem
import io.bennyoe.systems.AttackSystem
import io.bennyoe.systems.BasicSensorsSystem
import io.bennyoe.systems.BehaviorTreeSystem
import io.bennyoe.systems.CameraSystem
import io.bennyoe.systems.CloudSystem
import io.bennyoe.systems.CrowSystem
import io.bennyoe.systems.DamageSystem
import io.bennyoe.systems.DivingSystem
import io.bennyoe.systems.ExpireSystem
import io.bennyoe.systems.GameMoodSystem
import io.bennyoe.systems.GameStateSystem
import io.bennyoe.systems.HitStopSystem
import io.bennyoe.systems.InputSystem
import io.bennyoe.systems.JumpSystem
import io.bennyoe.systems.MoveSystem
import io.bennyoe.systems.ParticleRemoveSystem
import io.bennyoe.systems.RainSystem
import io.bennyoe.systems.SkySystem
import io.bennyoe.systems.StateSystem
import io.bennyoe.systems.TimeSystem
import io.bennyoe.systems.UiDataSystem
import io.bennyoe.systems.audio.AmbienceSystem
import io.bennyoe.systems.audio.MusicSystem
import io.bennyoe.systems.audio.ReverbSystem
import io.bennyoe.systems.audio.SoundEffectSystem
import io.bennyoe.systems.audio.UnderWaterSoundSystem
import io.bennyoe.systems.debug.BTBubbleSystem
import io.bennyoe.systems.debug.DamageTextSystem
import io.bennyoe.systems.debug.DebugPropsManager
import io.bennyoe.systems.debug.DebugSystem
import io.bennyoe.systems.debug.DebugUiBindingSystem
import io.bennyoe.systems.debug.DefaultDebugRenderService
import io.bennyoe.systems.debug.StateBubbleSystem
import io.bennyoe.systems.entitySpawn.CollisionSpawnSystem
import io.bennyoe.systems.entitySpawn.EntitySpawnSystem
import io.bennyoe.systems.light.AmbientLightSystem
import io.bennyoe.systems.light.EntityLightSystem
import io.bennyoe.systems.light.FlashlightSystem
import io.bennyoe.systems.physic.ContactHandlerSystem
import io.bennyoe.systems.physic.PhysicsSystem
import io.bennyoe.systems.physic.WaterSystem
import io.bennyoe.systems.render.PhysicTransformSyncSystem
import io.bennyoe.systems.render.RenderSystem
import io.bennyoe.systems.render.TransformVisualSyncSystem
import io.bennyoe.systems.render.UiRenderSystem
import io.bennyoe.ui.GameView
import ktx.assets.async.AssetStorage
import ktx.assets.disposeSafely
import ktx.box2d.createWorld
import ktx.inject.Context
import ktx.log.logger
import ktx.scene2d.Scene2DSkin
import kotlin.experimental.and
import kotlin.experimental.inv

class GameScreen(
    context: Context,
) : AbstractScreen(context) {
    private val profiler by lazy { GLProfiler(Gdx.graphics) }
    private val assets = context.inject<AssetStorage>()
    private val audio = context.inject<Audio>()
    private val worldObjectsAtlas = assets[TextureAssets.WORLD_OBJECTS_ATLAS.descriptor]
    private val waterAtlas = assets[TextureAssets.WATER_ATLAS.descriptor]
    private val cloudsAtlas = assets[TextureAssets.CLOUDS_ATLAS.descriptor]
    private val rainCloudsAtlas = assets[TextureAssets.RAIN_CLOUDS_ATLAS.descriptor]
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
    private val crowAtlases =
        TextureAtlases(
            assets[TextureAssets.CROW_ATLAS.descriptor],
            assets[TextureAssets.CROW_N_ATLAS.descriptor],
            assets[TextureAssets.CROW_S_ATLAS.descriptor],
        )
    private val particleAtlas = assets[TextureAssets.PARTICLE_ATLAS.descriptor]
    private val tiledMap = assets[MapAssets.TEST_MAP.descriptor]
    private val stages = context.inject<Stages>()
    private val stage = stages.stage
    private val uiStage = stages.uiStage
    private val gameView = GameView(Scene2DSkin.defaultSkin, profiler)
    private val spriteBatch = context.inject<SpriteBatch>()
    private val debugRenderService = DefaultDebugRenderService()
    private val polygonSpriteBatch = context.inject<PolygonSpriteBatch>()
    private val timeScaleCmp by lazy {
        with(entityWorld) { entityWorld.family { all(TimeScaleComponent) }.first()[TimeScaleComponent] }
    }
    private val debugCmp by lazy {
        with(entityWorld) { entityWorld.family { all(DebugComponent) }.first()[DebugComponent] }
    }
    private val phyWorld =
        createWorld(gravity = Vector2(0f, GRAVITY), true).apply {
            autoClearForces = false
        }

    // Framebuffer
    private var fbo: FrameBuffer? = null

    // container (provider) for the fbo, so that systems are getting a updated fbo every frame
    private lateinit var targets: RenderTargets

    private val rayHandler = RayHandler(phyWorld)
    private val lightEngine =
        Scene2dLightEngine(
            rayHandler = rayHandler,
            cam = stage.camera as OrthographicCamera,
            batch = spriteBatch,
            viewport = stage.viewport,
            stage = stage,
            entityCategory = EntityCategory.LIGHT.bit,
            entityMask = (EntityCategory.ALL.bit and EntityCategory.WORLD_BOUNDARY.bit.inv() and EntityCategory.SENSOR.bit.inv()),
            lightActivationRadius = 25f,
            lightViewportScale = 4f,
            refreshRateHz = 75f,
        )
    private val entityWorld by lazy {
        configureWorld {
            injectables {
                add("audio", audio)
                add("assetManager", assets)
                add("phyWorld", phyWorld)
                add("worldObjectsAtlas", worldObjectsAtlas)
                add("waterAtlas", waterAtlas)
                add("cloudsAtlas", cloudsAtlas)
                add("rainCloudsAtlas", rainCloudsAtlas)
                add("dawnAtlases", dawnAtlases)
                add("mushroomAtlases", mushroomAtlases)
                add("crowAtlases", crowAtlases)
                add("particlesAtlas", particleAtlas)
                add("stage", stage)
                add("uiStage", uiStage)
                add("shapeRenderer", ShapeRenderer())
                add("debugRenderService", debugRenderService)
                add("spriteBatch", spriteBatch)
                add("polygonSpriteBatch", polygonSpriteBatch)
                add("profiler", profiler)
                add("lightEngine", lightEngine)
                add("renderTargets", targets)
            }
            systems {
                add(AnimationSystem())
                add(EntitySpawnSystem())
                add(AmbientLightSystem())
                add(EntityLightSystem())
                add(FlashlightSystem())
                add(CollisionSpawnSystem())
                add(InputSystem())
                add(AttackSystem())
                add(GameStateSystem())
                add(DamageSystem())
                add(HitStopSystem())
                add(DamageTextSystem())
                add(JumpSystem())
                add(ContactHandlerSystem())
                add(WaterSystem())
                add(CrowSystem())
                add(PhysicsSystem())
                add(AmbienceSystem())
                add(ReverbSystem())
                add(CloudSystem())
                add(DivingSystem())
                add(RainSystem())
                add(UnderWaterSoundSystem())
                add(SoundEffectSystem())
                add(MusicSystem())
                add(BasicSensorsSystem())
                add(StateSystem())
                add(BehaviorTreeSystem())
                add(GameMoodSystem())
                add(TimeSystem())
                add(SkySystem())
                add(UiDataSystem())
                add(MoveSystem())
                add(PhysicTransformSyncSystem())
                add(TransformVisualSyncSystem())
                add(ParticleRemoveSystem())
                add(CameraSystem())
                add(RenderSystem())
                if (ENABLE_DEBUG) {
                    add(DebugSystem())
                    add(DebugUiBindingSystem())
                }
                add(ExpireSystem())
                add(StateBubbleSystem())
                add(BTBubbleSystem())
                add(UiRenderSystem())
            }
        }
    }

    override fun show() {
        DebugPropsManager.bind(debugRenderService)
        createFbo(Gdx.graphics.backBufferWidth, Gdx.graphics.backBufferHeight)
        targets = RenderTargets(requireNotNull(fbo))

        // input multiplexer
        val inputMultiplexer = InputMultiplexer()
        inputMultiplexer.addProcessor(uiStage)
        inputMultiplexer.addProcessor(PlayerInputProcessor(world = entityWorld))
        Gdx.input.inputProcessor = inputMultiplexer

        rayHandler.setBlurNum(2)
        profiler.enable()

        uiStage.isDebugAll = false

        uiStage.addActor(gameView)
        uiStage.addActor(gameView.debugWindow)
        gameView.debugWindow.setPosition(18f, uiStage.height - 18f, Align.topLeft)

        // setting basic graphic modes (can cause stutter on HiDPI displays)
        Gdx.graphics.setVSync(VSYNC)
        Gdx.graphics.setForegroundFPS(MAX_FPS)

        // add a gameState Entity to the screen
        entityWorld.entity {
            if (ENABLE_DEBUG) it += DebugComponent()
            it += GameStateComponent()
            it += CameraComponent()
            it += TimeScaleComponent()
        }

        // this adds all EventListenerSystems also to Scene2D
        entityWorld.systems.forEach { system ->
            when (system) {
                is EventListener -> stage.addListener(system)
                is CloudSystem -> system.initializeCloudPool()
            }
        }

        stage.fire(MapChangedEvent(tiledMap)) // mapChangeEvent gets fired

        super.show()
    }

    override fun render(delta: Float) {
        profiler.reset()
        val capped = delta.coerceAtMost(0.25f)
        val scale = timeScaleCmp.current
        val debugTimeScale = debugCmp.debugTimeScale
        val scaledDelta = capped * scale * debugTimeScale * TIME_SCALE

        GdxAI.getTimepiece().update(scaledDelta)
        entityWorld.update(scaledDelta)
    }

    override fun dispose() {
        worldObjectsAtlas.dispose()
        dawnAtlases.diffuseAtlas.dispose()
        dawnAtlases.normalAtlas?.dispose()
        dawnAtlases.specularAtlas?.dispose()
        mushroomAtlases.diffuseAtlas.dispose()
        mushroomAtlases.normalAtlas?.dispose()
        mushroomAtlases.specularAtlas?.dispose()
        crowAtlases.diffuseAtlas.dispose()
        crowAtlases.normalAtlas?.dispose()
        crowAtlases.specularAtlas?.dispose()
        entityWorld.dispose()
        fbo?.dispose()
        tiledMap.disposeSafely()
        audio.dispose()
    }

    override fun resize(
        width: Int,
        height: Int,
    ) {
        super.resize(width, height)
        lightEngine.resize(width, height)
        rayHandler.resizeFBO(Gdx.graphics.backBufferWidth / 2, Gdx.graphics.backBufferHeight / 2)
        createFbo(Gdx.graphics.backBufferWidth, Gdx.graphics.backBufferHeight)
        targets.fbo = requireNotNull(fbo)
    }

    private fun createFbo(
        width: Int,
        height: Int,
    ) {
        if (Gdx.graphics.width == 0 || Gdx.graphics.height == 0) return
        if (fbo != null) fbo!!.dispose()
        val frameBufferBuilder = GLFrameBuffer.FrameBufferBuilder(width, height)
        frameBufferBuilder.addBasicColorTextureAttachment(Pixmap.Format.RGBA8888)

        // add stencil-buffer to the fbo for masking the rain
        frameBufferBuilder.addStencilRenderBuffer(GL20.GL_STENCIL_INDEX8)
        fbo = frameBufferBuilder.build()
    }

    companion object {
        private val logger = logger<GameScreen>()
    }
}

/**
 * Container class for render targets used in the rendering pipeline.
 *
 * @property fbo The main [FrameBuffer] used for off-screen rendering.
 */
class RenderTargets(
    var fbo: FrameBuffer,
)
