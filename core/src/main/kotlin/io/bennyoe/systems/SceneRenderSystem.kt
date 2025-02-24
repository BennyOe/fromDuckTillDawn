package io.bennyoe.systems

import Tag
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Image as Scene2dImage
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import com.github.quillraven.fleks.World.Companion.inject
import io.bennyoe.components.Image

class SceneRenderSystem(
    private val stage: Stage = inject()
) : IteratingSystem(family { all(Image, Tag) }) {

    override fun onTick() {
        stage.viewport.apply()
        super.onTick()
    }

    override fun onTickEntity(entity: Entity) {
        val image = Scene2dImage(entity[Image].region)
        image.setSize(8f, 4.5f)

        with(stage) {
            addActor(image)
            act()
            draw()
        }
    }
}
