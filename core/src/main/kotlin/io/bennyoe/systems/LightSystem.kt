package io.bennyoe.systems

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import com.github.quillraven.fleks.World.Companion.inject
import io.bennyoe.components.LightComponent
import io.bennyoe.components.PhysicComponent
import io.bennyoe.event.MapChangedEvent
import io.bennyoe.lightEngine.core.GameLight
import io.bennyoe.lightEngine.core.Scene2dLightEngine
import ktx.log.logger

const val TRANSITION_DURATION = 4f

class LightSystem(
    private val lightEngine: Scene2dLightEngine = inject("lightEngine"),
) : IteratingSystem(family { all(LightComponent, PhysicComponent) }),
    EventListener {
    private var sunLight: GameLight.Directional? = null
    private var inTransition: Boolean = false
    private var currentDayTime = TimeOfDay.NIGHT
    private var transitionProgress = 0f

    private val dayData =
        TimeOfDayData(
            TimeOfDay.DAY,
            Color(0.75f, 0.5f, .4f, 1f),
            Color(0.2f, 0.28f, 0.2f, 1f),
            45f,
            4f,
            15f,
            0.9f,
            Color(0.95f, 0.98f, 1f, .5f),
        )

    private val nightData =
        TimeOfDayData(
            TimeOfDay.NIGHT,
            Color(0.15f, 0.23f, 0.45f, .8f),
            Color(0.15f, 0.23f, 0.45f, 1f),
            135f,
            5f,
            5f,
            1f,
            Color(0.1f, 0.18f, 0.25f, .4f),
        )

    private var from = nightData
    private var to = dayData

    override fun onTick() {
        if (inTransition) {
            transitionProgress = (transitionProgress + deltaTime / TRANSITION_DURATION).coerceAtMost(1f)

            val eased = Interpolation.sine.apply(transitionProgress)
            sunLight?.direction = MathUtils.lerpAngleDeg(from.direction, to.direction, eased)
            sunLight?.shaderLight?.elevation = MathUtils.lerp(from.elevation, to.elevation, eased)
            lightEngine.setNormalInfluence(MathUtils.lerp(from.normalInfluence, to.normalInfluence, eased))
            sunLight?.intensity = MathUtils.lerp(from.intensity, to.intensity, eased)

            sunLight?.color = interpolateColor(from.lightColor, to.lightColor, eased)

            val updatedAmbientColor = interpolateColor(from.ambientColor, to.ambientColor, eased)
            lightEngine.setBox2dAmbientLight(updatedAmbientColor)
            lightEngine.updateShaderAmbientColor(updatedAmbientColor)

            logger.debug { "direction: ${sunLight?.direction}" }

            if (transitionProgress >= 1f) {
                currentDayTime = if (currentDayTime == TimeOfDay.DAY) TimeOfDay.NIGHT else TimeOfDay.DAY
                inTransition = false
                logger.debug { "TimeOfDay successful changed" }
            }
        }

        super.onTick()
    }

    private fun interpolateColor(
        fromColor: Color,
        toColor: Color,
        eased: Float,
    ): Color {
        val r = MathUtils.lerp(fromColor.r, toColor.r, eased)
        val g = MathUtils.lerp(fromColor.g, toColor.g, eased)
        val b = MathUtils.lerp(fromColor.b, toColor.b, eased)
        val a = MathUtils.lerp(fromColor.a, toColor.a, eased)
        return Color(r, g, b, a)
    }

    override fun onTickEntity(entity: Entity) {
        val lightCmp = entity[LightComponent]
        val physicCmp = entity[PhysicComponent]
        when (val light = lightCmp.gameLight) {
            is GameLight.Spot -> light.position = physicCmp.body.position
            is GameLight.Point -> light.position = physicCmp.body.position
            else -> {}
        }
    }

    override fun handle(event: Event?): Boolean {
        when (event) {
            is MapChangedEvent -> {
                setupLights()
                return true
            }
        }
        return false
    }

    fun toggleTimeOfDay() {
        inTransition = true
        transitionProgress = 0f

        from = if (currentDayTime == TimeOfDay.DAY) dayData else nightData
        to = if (currentDayTime == TimeOfDay.DAY) nightData else dayData
    }

    private fun setupLights() {
        lightEngine.updateShaderAmbientColor(nightData.ambientColor.cpy())
        lightEngine.setBox2dAmbientLight(nightData.ambientColor.cpy())
        lightEngine.setDiffuseLight(true)
        lightEngine.setNormalInfluence(nightData.normalInfluence)
        lightEngine.setSpecularIntensity(.2f)
        lightEngine.setSpecularRemap(0.0f, 0.7f)

        sunLight =
            lightEngine.addDirectionalLight(
                nightData.lightColor.cpy(),
                nightData.direction,
                nightData.intensity,
                nightData.elevation,
                isManaged = false,
                isStatic = true,
                rays = 2048,
            )
    }

    companion object {
        val logger = logger<LightSystem>()
    }
}

enum class TimeOfDay { DAY, NIGHT }

enum class LightType {
    POINT_LIGHT,
    SPOT_LIGHT,
}

data class TimeOfDayData(
    val timeOfDay: TimeOfDay,
    val lightColor: Color,
    val ambientColor: Color,
    val direction: Float,
    val intensity: Float,
    val elevation: Float,
    val normalInfluence: Float,
    val tintColor: Color,
)
