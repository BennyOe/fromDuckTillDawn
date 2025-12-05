package io.bennyoe.systems

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.utils.Pool
import com.github.quillraven.fleks.IntervalSystem
import com.github.quillraven.fleks.World.Companion.inject
import io.bennyoe.event.CameraShakeEvent
import ktx.collections.GdxArray
import ktx.collections.isNotEmpty
import ktx.math.vec3

class CameraShake(
    var shakeDuration: Float = .6f,
    var maxShakeDistortion: Float = .3f,
) : Pool.Poolable {
    lateinit var camera: Camera
    var shakeDurationTimer: Float = 0f
    var storeCamPos = true
    var origCamPos = vec3()

    fun update(deltaTime: Float): Boolean {
        if (storeCamPos) {
            storeCamPos = false
            origCamPos = camera.position
        }

        if (shakeDurationTimer <= shakeDuration) {
            val currentPower = maxShakeDistortion * ((shakeDuration - shakeDurationTimer) / shakeDuration)
            camera.position.x = origCamPos.x + MathUtils.random(-1f, 1f) * currentPower
            camera.position.y = origCamPos.y + MathUtils.random(-1f, 1f) * currentPower
            camera.update()

            shakeDurationTimer += deltaTime
            return false
        }

        camera.position.set(origCamPos)
        camera.update()
        return true
    }

    override fun reset() {
        shakeDurationTimer = 0f
        storeCamPos = true
        origCamPos = vec3().setZero()
    }
}

class CameraShakePool(
    val camera: Camera,
) : Pool<CameraShake>() {
    override fun newObject(): CameraShake =
        CameraShake().apply {
            this.camera = this@CameraShakePool.camera
        }
}

class CameraShakeSystem(
    val stage: Stage = inject("stage"),
) : IntervalSystem(),
    EventListener {
    val cameraShakePool = CameraShakePool(stage.camera)
    val activeShakes = GdxArray<CameraShake>(4)

    override fun onTick() {
        if (activeShakes.isNotEmpty()) {
            val shake = activeShakes.first()
            if (shake.update(deltaTime)) {
                activeShakes.removeIndex(0)
                cameraShakePool.free(shake)
            }
        }
    }

    override fun handle(event: Event): Boolean {
        when (event) {
            is CameraShakeEvent -> {
                if (activeShakes.size < 4) {
                    activeShakes.add(cameraShakePool.obtain())
                }
                return true
            }

            else -> {
                return false
            }
        }
    }
}
