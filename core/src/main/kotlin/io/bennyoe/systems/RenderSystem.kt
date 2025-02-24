package io.bennyoe.systems

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.viewport.Viewport
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import com.github.quillraven.fleks.World.Companion.inject
import io.bennyoe.components.Image
import ktx.graphics.use

class RenderSystem(
    private val spriteBatch: SpriteBatch = inject(),
    private val viewport: Viewport = inject("gameViewport")
) : IteratingSystem(family { all(Image) }) {

    override fun onTick() {
        spriteBatch.projectionMatrix = viewport.camera.combined
        super.onTick()
    }

    override fun onTickEntity(entity: Entity) {
        with(spriteBatch) {
            use {
                it.draw(entity[Image].sprite, 2f, 2f, 2f, 2f)
            }
        }
    }

}
