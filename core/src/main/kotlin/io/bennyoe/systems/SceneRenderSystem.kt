package io.bennyoe.systems

import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import com.github.quillraven.fleks.World.Companion.inject
import io.bennyoe.components.ImageComponent

class SceneRenderSystem(
    private val stage: Stage = inject()
) : IteratingSystem(family { all(ImageComponent) }) {

    override fun onTick() {
        with(stage) {
            viewport.apply()
            act(deltaTime)
        }
        super.onTick()
        stage.draw()
    }

    override fun onTickEntity(entity: Entity) {
        val imageComponent = entity[ImageComponent]
        imageComponent.image.setSize(imageComponent.scaleX, imageComponent.scaleY)
    }
}
