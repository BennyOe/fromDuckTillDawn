package io.bennyoe.systems

import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import com.github.quillraven.fleks.World.Companion.inject
import io.bennyoe.components.ImageComponent
import io.bennyoe.components.PlayerComponent
import io.bennyoe.event.MapChangedEvent
import ktx.tiled.height
import ktx.tiled.width
import kotlin.math.max
import kotlin.math.min

class CameraSystem(
    stage: Stage = inject()
) : IteratingSystem(family { all(ImageComponent, PlayerComponent) }), EventListener {
    private val camera = stage.camera
    private var maxW = 0f
    private var maxH = 0f

    override fun onTickEntity(entity: Entity) {
        val imageCmps = entity[ImageComponent]
        // we center on the image because it has an
        // interpolated position for rendering which makes
        // the game smoother
        val (xPos, yPos) = calculateCameraPosition(imageCmps.image)

        camera.position.set(xPos, yPos, camera.position.z)
    }

    override fun handle(event: Event): Boolean {
        when (event) {
            is MapChangedEvent -> {
                maxW = event.map.width.toFloat()
                maxH = event.map.height.toFloat()
                return true
            }
        }
        return false
    }

    private fun calculateCameraPosition(image: Image): Pair<Float, Float> {
        val viewW = camera.viewportWidth * 0.5f
        val viewH = camera.viewportHeight * 0.5f

        // set borders for the camera that it stays inside the map
        val camMinW = min(viewW, maxW - viewW)
        val camMaxW = max(viewW, maxW - viewW)
        val camMinH = min(viewH, maxH - viewH)
        val camMaxH = max(viewH, maxH - viewH)

        // for not jumping with the camera when image gets flipped
        val xPos = (image.x + image.width * 0.5f).coerceIn(camMinW, camMaxW)
        val yPos = (image.y + image.height * 0.5f).coerceIn(camMinH, camMaxH)

        return Pair(xPos, yPos)
    }

}
