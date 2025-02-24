package io.bennyoe.screens

import Tag
import Tog
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.utils.viewport.FitViewport
import com.badlogic.gdx.utils.viewport.StretchViewport
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.configureWorld
import io.bennyoe.components.Image
import io.bennyoe.systems.RenderSystem
import io.bennyoe.systems.SceneRenderSystem
import ktx.app.KtxScreen
import ktx.app.clearScreen
import ktx.inject.Context
import ktx.log.logger

private val LOG = logger<GameScreen>()

class GameScreen(
    context: Context,
) : KtxScreen {
    private val textureAtlas = TextureAtlas("textures/player.atlas")
    private val gameViewport by lazy { FitViewport(16f, 9f) }
    private val extendViewport by lazy { StretchViewport(16f, 9f) }
    private val stage = Stage(extendViewport)
    private val world = configureWorld {
        injectables {
            add(context.inject<SpriteBatch>())
            add("gameViewport", gameViewport)
            add(stage)
        }
        families {
        }
        systems {
            add(SceneRenderSystem())
            add(RenderSystem())
        }
        onAddEntity { entity: Entity ->
            LOG.info { "On Add called" }
        }
        onRemoveEntity { entity: Entity ->
            LOG.info { "On Remove called" }
        }
    }

    override fun show() {
        world.entity {
            it += Image((TextureRegion(textureAtlas.findRegion("attack01/attack01"))))
            it += Tog
        }
        world.entity {
            it += Image((TextureRegion(textureAtlas.findRegion("walking01/walking01"))))
            it += Tag
        }
        super.show()
    }

    override fun render(delta: Float) {
        clearScreen(red = 0.7f, green = 0.7f, blue = 0.7f)
        world.update(delta)
    }

    override fun resize(width: Int, height: Int) {
        super.resize(width, height)
        gameViewport.update(width, height, true)
        extendViewport.update(width, height, true)
    }

    override fun dispose() {
        world.dispose()
    }
}
