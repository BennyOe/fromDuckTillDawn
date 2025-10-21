package io.bennyoe.systems

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.math.MathUtils.lerp
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import com.github.quillraven.fleks.World.Companion.inject
import io.bennyoe.components.CameraComponent
import io.bennyoe.components.ImageComponent
import io.bennyoe.components.PlayerComponent
import io.bennyoe.components.TransformComponent
import io.bennyoe.config.GameConstants.CAMERA_SMOOTHING_FACTOR
import io.bennyoe.event.MapChangedEvent
import io.bennyoe.systems.debug.DebugType
import io.bennyoe.systems.debug.DefaultDebugRenderService
import io.bennyoe.systems.debug.addToDebugView
import ktx.log.logger
import ktx.math.vec2
import ktx.tiled.height
import ktx.tiled.width

private const val DEADZONE_WIDTH = 6f
private const val DEADZONE_HEIGHT = 6f

class CameraSystem(
    stage: Stage = inject("stage"),
    val debugRenderService: DefaultDebugRenderService = inject("debugRenderService"),
) : IteratingSystem(family { all(ImageComponent, PlayerComponent, TransformComponent) }),
    EventListener {
    private val camera = stage.camera as OrthographicCamera
    private var maxW = 0f
    private var maxH = 0f
    private val deadZone = Rectangle(0f, 0f, DEADZONE_WIDTH, DEADZONE_HEIGHT)
    private val lookaheadLeftZone = Rectangle(0f, 0f, 1f, DEADZONE_HEIGHT)
    private val lookaheadRightZone = Rectangle(0f, 0f, 1f, DEADZONE_HEIGHT)
    private val cameraEntity by lazy { world.family { all(CameraComponent) }.first() }
    private var initialCameraSetup = true
    private var newCameraPos = vec2()

    override fun onTickEntity(entity: Entity) {
        val imageCmp = entity[ImageComponent]

        val playerX = imageCmp.image.x + imageCmp.image.width / 2f
        val playerY = imageCmp.image.y + imageCmp.image.height / 2f

        // Calculate Deadzone position based on current camera position, including the Y offset
        deadZone.setCenter(camera.position.x, camera.position.y)
        lookaheadLeftZone.setCenter(deadZone.x + 0.3f, deadZone.y + DEADZONE_HEIGHT / 2f)
        lookaheadRightZone.setCenter(deadZone.x + DEADZONE_WIDTH - 0.3f, deadZone.y + DEADZONE_HEIGHT / 2f)

        calculateDeadzone(playerX, playerY)
        clampCameraInMapDimensions(newCameraPos)

        camera.position.set(
            lerp(camera.position.x, newCameraPos.x, CAMERA_SMOOTHING_FACTOR),
            lerp(camera.position.y, newCameraPos.y, CAMERA_SMOOTHING_FACTOR),
            camera.position.z,
        )

        if (initialCameraSetup) {
            camera.position.set(playerX, playerY + 2f, 0f)
            initialCameraSetup = false
        }

        // Update deadzone rect for debug drawing (after camera moved, considering offset)
        deadZone.addToDebugView(debugRenderService, Color.CYAN, "camera deadzone", debugType = DebugType.CAMERA)
        lookaheadLeftZone.addToDebugView(debugRenderService, Color.ORANGE, "left", debugType = DebugType.CAMERA)
        lookaheadRightZone.addToDebugView(debugRenderService, Color.ORANGE, "right", debugType = DebugType.CAMERA)
    }

    private fun clampCameraInMapDimensions(newCameraPos: Vector2) {
        val viewW = camera.viewportWidth * camera.zoom * 0.5f
        val viewH = camera.viewportHeight * camera.zoom * 0.5f

        val maxX = maxW - viewW
        val maxY = maxH - viewH

        newCameraPos.x = newCameraPos.x.coerceIn(viewW.coerceAtMost(maxX), maxX.coerceAtLeast(viewW))
        newCameraPos.y = newCameraPos.y.coerceIn(viewH.coerceAtMost(maxY), maxY.coerceAtLeast(viewH))
    }

    private fun calculateDeadzone(
        playerX: Float,
        playerY: Float,
    ) {
        var cameraDeltaX = 0f
        var cameraDeltaY = 0f

        // Check horizontal bounds
        if (playerX < deadZone.x) {
            cameraDeltaX = playerX - deadZone.x
        } else if (playerX > deadZone.x + deadZone.width) {
            cameraDeltaX = playerX - (deadZone.x + deadZone.width)
        }

        // Check vertical bounds
        if (playerY < deadZone.y) {
            cameraDeltaY = playerY - deadZone.y
        } else if (playerY > deadZone.y + deadZone.height) {
            cameraDeltaY = playerY - (deadZone.y + deadZone.height)
        }

        // Apply movement only if outside deadzone
        newCameraPos.x = camera.position.x + cameraDeltaX
        newCameraPos.y = camera.position.y + cameraDeltaY
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

    companion object {
        val logger = logger<CameraSystem>()
    }
}
