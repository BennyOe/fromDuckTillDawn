package io.bennyoe.systems

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.math.MathUtils.lerp
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import com.github.quillraven.fleks.World.Companion.inject
import io.bennyoe.components.CameraComponent
import io.bennyoe.components.GameStateComponent
import io.bennyoe.components.ImageComponent
import io.bennyoe.components.PlayerComponent
import io.bennyoe.config.GameConstants.CAMERA_SMOOTHING_FACTOR
import io.bennyoe.event.MapChangedEvent
import io.bennyoe.service.DefaultDebugRenderService
import io.bennyoe.service.addToDebugView
import io.bennyoe.systems.debug.DebugType
import ktx.log.logger
import ktx.tiled.height
import ktx.tiled.width
import kotlin.math.max
import kotlin.math.min

class CameraSystem(
    stage: Stage = inject("stage"),
    val debugRenderService: DefaultDebugRenderService = inject("debugRenderService"),
) : IteratingSystem(family { all(ImageComponent, PlayerComponent) }),
    EventListener {
    private val camera = stage.camera as OrthographicCamera
    private var maxW = 0f
    private var maxH = 0f
    private var cameraTargetX = 0f
    val deadzone = Rectangle(0f, 0f, 1f, 1f)
    private val gameStateEntity by lazy { world.family { all(GameStateComponent) }.first() }
    private val cameraEntity by lazy { world.family { all(CameraComponent) }.first() }

    override fun onTickEntity(entity: Entity) {
        val imageCmps = entity[ImageComponent]
        val gameStateCmp = gameStateEntity[GameStateComponent]
        // we center on the image because it has an
        // interpolated position for rendering which makes
        // the game smoother
        val (xPos, yPos) = calculateCameraPosition(imageCmps, gameStateCmp.isLightingEnabled)

        camera.position.set(xPos, yPos, camera.position.z)

        deadzone.set(camera.position.x - 1f, camera.position.y - 1f, 2f, 4f)
        deadzone.addToDebugView(debugRenderService, Color.CYAN, "camera deadzone", debugType = DebugType.CAMERA)
    }

    override fun onTick() {
        val cameraComponent = cameraEntity.getOrNull(CameraComponent)
        cameraComponent?.let { camera.zoom = it.zoomFactor }
        super.onTick()
        camera.update()
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

    private fun calculateCameraPosition(
        imageCmp: ImageComponent,
        isLightingEnabled: Boolean,
    ): Pair<Float, Float> {
        val viewW = camera.viewportWidth * 0.5f
        val viewH = camera.viewportHeight * 0.5f

        // set map boundaries for the camera
        val camMinW = min(viewW, maxW - viewW)
        val camMaxW = max(viewW, maxW - viewW)
        val camMinH = min(viewH, maxH - viewH)
        val camMaxH = max(viewH, maxH - viewH)

        // this is needed as long as the lighting engine can switched off. TODO remove else when not having switch
        val desiredX =
            if (imageCmp.flipImage && isLightingEnabled) {
                imageCmp.image.x
            } else {
                imageCmp.image.x + imageCmp.image.width
            }

//        Circle(desiredX, 3.8f, 0.2f).addToDebugView(debugRenderService, Color.RED, "player")
        cameraTargetX = lerp(cameraTargetX, desiredX, CAMERA_SMOOTHING_FACTOR)

        val clampedX = cameraTargetX.coerceIn(camMinW, camMaxW)

        val yPos = (imageCmp.image.y + imageCmp.image.height * 0.5f).coerceIn(camMinH, camMaxH)
        return clampedX to yPos
    }

    companion object {
        val logger = logger<CameraSystem>()
    }
}
