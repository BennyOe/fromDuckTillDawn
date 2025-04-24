package io.bennyoe.components

import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World

class ImageComponent(
    private val stage: Stage,
    val scaleX: Float = 1f,
    val scaleY: Float = 1f,
    var flipImage: Boolean = false
) : Component<ImageComponent> {
    lateinit var image: Image
    override fun type(): ComponentType<ImageComponent> = ImageComponent

    override fun World.onAdd(entity: Entity) {
        // if image is not in stage -> add image to stage
        if (!stage.actors.contains(image, true)) {
            stage.addActor(image)
        }
    }

    override fun World.onRemove(entity: Entity) {
        image.remove()
        this.dispose()
    }

    companion object : ComponentType<ImageComponent>() {
    }
}
