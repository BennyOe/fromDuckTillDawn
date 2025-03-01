package io.bennyoe.screens

import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.BodyDef
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.github.quillraven.fleks.configureWorld
import io.bennyoe.PlayerInputProcessor
import io.bennyoe.components.AnimationComponent
import io.bennyoe.components.AnimationType
import io.bennyoe.components.ImageComponent
import io.bennyoe.components.PhysicComponent
import io.bennyoe.systems.AnimationSystem
import io.bennyoe.systems.DebugSystem
import io.bennyoe.systems.PhysicsSystem
import io.bennyoe.systems.RenderSystem
import ktx.app.KtxScreen
import ktx.box2d.createWorld
import ktx.inject.Context
import ktx.log.logger

class GameScreen(
    context: Context,
) : KtxScreen {
    private val textureAtlas = TextureAtlas("textures/player.atlas")
    private val stage = context.inject<Stage>()
    private val phyWorld = createWorld(gravity = Vector2(0f, 0f), true).apply {
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
            add(PhysicsSystem())
            add(RenderSystem())
            add(DebugSystem())
        }
    }

    override fun show() {
        PlayerInputProcessor()
        entityWorld.entity {
            val animation = AnimationComponent()
            animation.nextAnimation(AnimationType.IDLE)
            it += animation

            val image = ImageComponent(stage, 2f, 1f).apply {
                image = Image().apply {
                    setPosition(2f, 2f)
                    setSize(1f, 1f)
                }
            }
            it += image

                LOG.debug { "ImageComponent x,y ${image.scaleX} ${image.scaleY} Image x,y ${image.image.width} ${image.image.height}" }
            val physics = PhysicComponent.physicsComponentFromImage(
                phyWorld,
                image.image,
                BodyDef.BodyType.DynamicBody,
            )
            it += physics
        }
        super.show()
    }

    override fun render(delta: Float) {
        entityWorld.update(delta.coerceAtMost(0.25f))
    }

    override fun dispose() {
        textureAtlas.dispose()
        entityWorld.dispose()
    }

    companion object {
        private val LOG = logger<GameScreen>()
    }
}
