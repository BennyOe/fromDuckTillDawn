package io.bennyoe.screens

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.utils.viewport.FitViewport
import com.github.quillraven.fleks.configureWorld
import io.bennyoe.components.AnimationComponent
import io.bennyoe.components.AnimationType
import io.bennyoe.components.ImageComponent
import io.bennyoe.systems.AnimationSystem
import io.bennyoe.systems.SceneRenderSystem
import ktx.app.KtxScreen
import ktx.app.clearScreen
import ktx.inject.Context

class GameScreen(
    context: Context,
) : KtxScreen {
    private val textureAtlas = TextureAtlas("textures/player.atlas")
    private val stage = context.inject<Stage>()
    private val gameViewport = context.inject<FitViewport>()
    private val world = configureWorld {
        injectables {
            add(context.inject<SpriteBatch>())
            add("gameViewport", gameViewport)
            add(stage)
        }
        systems {
            add(AnimationSystem(textureAtlas))
            add(SceneRenderSystem())
        }
    }

    override fun show() {
       world.entity {
            val player = AnimationComponent()
            player.nextAnimation(AnimationType.WALKING01)

            it += player

            val image = ImageComponent(stage, 2f, 1f)
            image.image = Image()
            it += image
        }
        super.show()
    }

    override fun render(delta: Float) {
        clearScreen(red = 0.7f, green = 0.7f, blue = 0.7f)
        world.update(delta)
    }

    override fun dispose() {
        textureAtlas.dispose()
        world.dispose()
    }
}
