package io.bennyoe.screens

import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.maps.tiled.TiledMap
import com.badlogic.gdx.maps.tiled.TmxMapLoader
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.quillraven.fleks.configureWorld
import io.bennyoe.event.MapChangedEvent
import io.bennyoe.event.fire
import io.bennyoe.systems.AnimationSystem
import io.bennyoe.systems.CameraSystem
import io.bennyoe.systems.CollisionSpawnSystem
import io.bennyoe.systems.DebugSystem
import io.bennyoe.systems.MoveSystem
import io.bennyoe.systems.PhysicsSystem
import io.bennyoe.systems.RenderSystem
import io.bennyoe.systems.EntitySpawnSystem
import ktx.app.KtxScreen
import ktx.assets.disposeSafely
import ktx.box2d.createWorld
import ktx.inject.Context
import ktx.log.logger

class GameScreen(
    context: Context,
) : KtxScreen {
    //TODO implement asset manager
    private val textureAtlas = TextureAtlas("textures/player.atlas")
    private val stage = context.inject<Stage>()
    private var tiledMap: TiledMap? = null
    private val phyWorld = createWorld(gravity = Vector2(0f, -9.81f), true).apply {
        autoClearForces = false
    }
    private val entityWorld = configureWorld {
        injectables {
            add("phyWorld", phyWorld)
            add(textureAtlas)
            add(stage)
        }
        systems {
            add(AnimationSystem())
            add(EntitySpawnSystem())
            add(CollisionSpawnSystem())
            add(PhysicsSystem())
            add(MoveSystem())
            add(CameraSystem())
            add(RenderSystem())
            add(DebugSystem())
        }
    }

    override fun show() {

        // this adds all EventListenerSystems also to Scene2D
        entityWorld.systems.forEach { system ->
            if (system is EventListener){
                stage.addListener(system)
            }
        }

        tiledMap = TmxMapLoader().load("map/inTheWoods/inTheWoods1.tmx") // map gets loaded
        stage.fire(MapChangedEvent(tiledMap!!)) // mapChangeEvent gets fired

        super.show()
    }

    override fun render(delta: Float) {
        entityWorld.update(delta.coerceAtMost(0.25f))
    }

    override fun dispose() {
        textureAtlas.dispose()
        entityWorld.dispose()
        tiledMap?.disposeSafely()
    }

    companion object {
        private val LOG = logger<GameScreen>()
    }
}
