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
import io.bennyoe.components.PhysicComponent
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
import kotlin.math.abs
import kotlin.math.sign

private const val DEADZONE_WIDTH = 6f
private const val DEADZONE_HEIGHT = 6f
private const val LOOKAHEAD_DISTANCE = 9f
private const val LOOKAHEAD_VELOCITY_THRESHOLD = 0.1f
private const val LOOKAHEAD_SMOOTHING_FACTOR = 0.05f
private const val CAMERA_Y_OFFSET = 3f
private const val CAMERA_INITIAL_Y_OFFSET = 10f // is needed to initialize the camera on the correct posistion

class CameraSystem(
    stage: Stage = inject("stage"),
    val debugRenderService: DefaultDebugRenderService = inject("debugRenderService"),
) : IteratingSystem(family { all(ImageComponent, PlayerComponent, TransformComponent) }),
    EventListener {
    private val camera = stage.camera as OrthographicCamera
    private var maxW = 0f
    private var maxH = 0f
    private val deadZone = Rectangle(0f, 0f, DEADZONE_WIDTH, DEADZONE_HEIGHT)
    private val cameraEntity by lazy { world.family { all(CameraComponent) }.first() }
    private var initialCameraSetup = true
    private var newCameraPos = vec2()
    private var currentLookaheadOffset = 0f

    override fun onTickEntity(entity: Entity) {
        val imageCmp = entity[ImageComponent]
        val physicCmp = entity[PhysicComponent]

        val playerX = imageCmp.image.x + imageCmp.image.width / 2f
        val playerY = imageCmp.image.y + imageCmp.image.height / 2f

        deadZone.setCenter(camera.position.x, camera.position.y - CAMERA_Y_OFFSET)

        val moveDirX =
            if (abs(physicCmp.body.linearVelocity.x) > LOOKAHEAD_VELOCITY_THRESHOLD) {
                sign(physicCmp.body.linearVelocity.x)
            } else {
                0f
            }
        val targetLookaheadOffset = moveDirX * LOOKAHEAD_DISTANCE

        currentLookaheadOffset =
            lerp(
                currentLookaheadOffset,
                targetLookaheadOffset,
                LOOKAHEAD_SMOOTHING_FACTOR,
            )

        val targetX = playerX + currentLookaheadOffset

        calculateCameraMovement(targetX, playerY)
        clampCameraInMapDimensions(newCameraPos)

        camera.position.set(
            lerp(camera.position.x, newCameraPos.x, CAMERA_SMOOTHING_FACTOR),
            lerp(camera.position.y, newCameraPos.y, CAMERA_SMOOTHING_FACTOR),
            camera.position.z,
        )

        if (initialCameraSetup) {
            camera.position.set(playerX, playerY + CAMERA_Y_OFFSET + CAMERA_INITIAL_Y_OFFSET, 0f)
            initialCameraSetup = false
        }

        deadZone.addToDebugView(debugRenderService, Color.CYAN, "camera deadzone", debugType = DebugType.CAMERA)
    }

    private fun clampCameraInMapDimensions(newCameraPos: Vector2) {
        val viewW = camera.viewportWidth * camera.zoom * 0.5f
        val viewH = camera.viewportHeight * camera.zoom * 0.5f

        val maxX = maxW - viewW
        val maxY = maxH - viewH

        newCameraPos.x = newCameraPos.x.coerceIn(viewW.coerceAtMost(maxX), maxX.coerceAtLeast(viewW))
        newCameraPos.y = newCameraPos.y.coerceIn(viewH.coerceAtMost(maxY), maxY.coerceAtLeast(viewH))
    }

    private fun calculateCameraMovement(
        targetX: Float,
        targetY: Float,
    ) {
        var cameraDeltaX = 0f
        var cameraDeltaY = 0f

        // Check horizontal bounds (using targetX)
        if (targetX < deadZone.x) {
            cameraDeltaX = targetX - deadZone.x
        } else if (targetX > deadZone.x + deadZone.width) {
            cameraDeltaX = targetX - (deadZone.x + deadZone.width)
        }

        // Check vertical bounds (using targetY)
        if (targetY < deadZone.y) {
            cameraDeltaY = targetY - deadZone.y
        } else if (targetY > deadZone.y + deadZone.height) {
            cameraDeltaY = targetY - (deadZone.y + deadZone.height)
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
