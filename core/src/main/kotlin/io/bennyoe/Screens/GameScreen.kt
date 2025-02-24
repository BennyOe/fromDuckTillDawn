package io.bennyoe.Screens

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.utils.viewport.FitViewport
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.configureWorld
import io.bennyoe.components.Image
import io.bennyoe.systems.RenderSystem
import io.bennyoe.systems.SceneRenderSystem
import ktx.app.KtxScreen
import ktx.app.clearScreen
import ktx.assets.toInternalFile
import ktx.inject.Context
import ktx.log.logger

private val LOG = logger<GameScreen>()

class GameScreen(
    context: Context,
) : KtxScreen {
    private val gameViewport by lazy { FitViewport(16f, 9f) }
    private val stage = Stage(gameViewport)
    private val world = configureWorld {
        injectables {
            add(context.inject<SpriteBatch>())
            add("gameViewport", gameViewport)
            add(stage)
        }
        families {
        }
        systems {
            add(RenderSystem())
            add(SceneRenderSystem())
        }
        onAddEntity { entity: Entity ->

        }
        onRemoveEntity { entity: Entity ->

        }
    }

    override fun show() {
        world.entity {
            it += Image(Sprite(Texture("map.png".toInternalFile())))
        }
        super.show()
    }

    override fun render(delta: Float) {
        clearScreen(red = 0.7f, green = 0.7f, blue = 0.7f)
        world.update(delta)
    }

    override fun resize(width: Int, height: Int) {
        gameViewport.update(width, height)
    }


    override fun dispose() {
        world.dispose()
    }
}
