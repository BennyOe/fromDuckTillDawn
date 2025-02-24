package io.bennyoe.systems

import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import com.github.quillraven.fleks.World.Companion.inject
import io.bennyoe.components.Image

class SceneRenderSystem(
    private val stage: Stage = inject()
) : IteratingSystem(family { all(Image) }) {

    override fun onTickEntity(entity: Entity) {
        val image = com.badlogic.gdx.scenes.scene2d.ui.Image(entity[Image].sprite)
        image.setSize(2f, 2f)
        stage.addActor(image)

        with(stage) {
            stage.act()
            stage.draw()
        }
    }
}
