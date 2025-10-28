package io.bennyoe.systems

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import com.github.quillraven.fleks.World.Companion.inject
import io.bennyoe.components.ParallaxComponent
import io.bennyoe.components.TransformComponent
import ktx.log.logger

class ParallaxSystem(
    val stage: Stage = inject("stage"),
) : IteratingSystem(family { all(ParallaxComponent, TransformComponent) }) {
    private val camera = stage.camera as OrthographicCamera

    private var initialCameraReferenceX: Float = 0f
    private var initialCameraReferenceY: Float = 0f

    override fun onInit() {
        initialCameraReferenceX = camera.position.x
        initialCameraReferenceY = camera.position.y
    }

    override fun onTickEntity(entity: Entity) {
        val parallaxCmp = entity[ParallaxComponent]
        val transformCmp = entity[TransformComponent]

        val cameraX = camera.position.x
        val cameraY = camera.position.y

        val parallaxFactorX = parallaxCmp.factor.x
        val parallaxFactorY = parallaxCmp.factor.y

        val initialX = parallaxCmp.initialPosition.x
        val initialY = parallaxCmp.initialPosition.y

        // Calculate how much the camera has moved *from its initial reference point*
        val cameraDisplacementX = cameraX - initialCameraReferenceX
        val cameraDisplacementY = cameraY - initialCameraReferenceY

        // Calculate how much this layer should move *relative to the camera's movement*
        val layerDisplacementX = cameraDisplacementX * (1 - parallaxFactorX)
        val layerDisplacementY = cameraDisplacementY * (1 - parallaxFactorY)

        // Final position = Initial Position + Calculated Layer Displacement
        transformCmp.position.x = initialX + layerDisplacementX
        transformCmp.position.y = initialY + layerDisplacementY
    }

    companion object {
        val logger = logger<ParallaxSystem>()
    }
}
