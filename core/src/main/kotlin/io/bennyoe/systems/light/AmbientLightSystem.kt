package io.bennyoe.systems.light

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.github.quillraven.fleks.IntervalSystem
import com.github.quillraven.fleks.World.Companion.inject
import io.bennyoe.components.GameStateComponent
import io.bennyoe.config.GameConstants.DYNAMIC_TIME_OF_DAY
import io.bennyoe.config.GameConstants.INITIAL_TIME_OF_DAY
import io.bennyoe.event.AmbienceChangeEvent
import io.bennyoe.event.MapChangedEvent
import io.bennyoe.lightEngine.core.GameLight
import io.bennyoe.lightEngine.core.Scene2dLightEngine
import io.bennyoe.utility.times

internal data class LightingParameters(
    var directionalLightIntensity: Float = 0.75f,
    var box2dLightStrength: Float = 1f,
    var shaderAmbientStrength: Float = 1.2f,
    var shaderIntensity: Float = 0.8f,
    var useDiffuseLight: Boolean = true,
    var normalInfluence: Float = 1f,
    var specularIntensity: Float = 0.7f,
    var sunElevation: Float = 45f,
)

class AmbientLightSystem(
    private val lightEngine: Scene2dLightEngine = inject("lightEngine"),
) : IntervalSystem(),
    EventListener {
    private val gameStateCmp by lazy { world.family { all(GameStateComponent) }.first()[GameStateComponent] }
    private val timeOfDayManager: TimeOfDayManager = TimeOfDayManager()
    private val weatherManager: WeatherManager = WeatherManager()
    private var sunLight: GameLight.Directional? = null

    private val outdoorParams =
        LightingParameters(
            directionalLightIntensity = 0.75f,
            box2dLightStrength = 1f,
            shaderAmbientStrength = 1.2f,
            shaderIntensity = 0.8f,
            useDiffuseLight = true,
            normalInfluence = 1f,
            specularIntensity = 0.7f,
            sunElevation = 45f,
        )
    private val indoorParams =
        LightingParameters(
            directionalLightIntensity = 1f,
            box2dLightStrength = 0.4f,
            shaderAmbientStrength = 0.5f,
            shaderIntensity = 0.7f,
            normalInfluence = 0.81f,
            specularIntensity = 0.7f,
        )

    internal var currentParams = outdoorParams
    private var sourceParams: LightingParameters? = null
    private var targetParams: LightingParameters? = null
    private val transitionDuration: Float = 2f
    private var transitionTimer: Float = transitionDuration

    override fun onTick() {
        if (transitionTimer < transitionDuration) {
            transitionTimer += deltaTime
            val alpha = (transitionTimer / transitionDuration).coerceIn(0f, 1f)
            if (sourceParams != null && targetParams != null) {
                currentParams = lerpParameters(sourceParams!!, targetParams!!, Interpolation.smoother.apply(alpha))
            }
        }
        weatherManager.update(deltaTime, gameStateCmp.weather)
        val timeProps = timeOfDayManager.getProperties(gameStateCmp.timeOfDay)
        val weatherFactors = weatherManager.getFactors()

        lightEngine.setDiffuseLight(currentParams.useDiffuseLight)

        applyLightingData(timeProps, weatherFactors)
    }

    override fun handle(event: Event?): Boolean {
        when (event) {
            is MapChangedEvent -> {
                setupLights()
                currentParams = outdoorParams.copy() // Start with default outdoor settings
                return true
            }

            is AmbienceChangeEvent -> {
                val newTarget = if (event.isIndoor) indoorParams.copy() else outdoorParams.copy()
                // Don't start a new transition if we are already heading to the same target
                if (targetParams != newTarget) {
                    sourceParams = currentParams.copy() // Start fade from the current state
                    targetParams = newTarget
                    transitionTimer = 0f // Reset timer to start the fade
                }
                return true
            }
        }
        return false
    }

    private fun lerpParameters(
        from: LightingParameters,
        to: LightingParameters,
        alpha: Float,
    ): LightingParameters =
        LightingParameters(
            directionalLightIntensity = MathUtils.lerp(from.directionalLightIntensity, to.directionalLightIntensity, alpha),
            shaderAmbientStrength = MathUtils.lerp(from.shaderAmbientStrength, to.shaderAmbientStrength, alpha),
            box2dLightStrength = MathUtils.lerp(from.box2dLightStrength, to.box2dLightStrength, alpha),
            shaderIntensity = MathUtils.lerp(from.shaderIntensity, to.shaderIntensity, alpha),
            normalInfluence = MathUtils.lerp(from.normalInfluence, to.normalInfluence, alpha),
            specularIntensity = MathUtils.lerp(from.specularIntensity, to.specularIntensity, alpha),
            sunElevation = MathUtils.lerp(from.sunElevation, to.sunElevation, alpha),
            // Boolean values switch instantly at the end of the transition
            useDiffuseLight = if (alpha >= 1f) to.useDiffuseLight else from.useDiffuseLight,
        )

    private fun setupLights() {
        val initialData = timeOfDayManager.getProperties(INITIAL_TIME_OF_DAY)
        lightEngine.updateShaderAmbientColor(initialData.ambientColor.cpy())
        lightEngine.setBox2dAmbientLight(initialData.ambientColor.cpy())
        lightEngine.setDiffuseLight(true)
        lightEngine.setNormalInfluence(initialData.normalInfluence)
        lightEngine.setSpecularIntensity(1f)
        lightEngine.setSpecularRemap(0.0f, 0.7f)

        sunLight =
            lightEngine.addDirectionalLight(
                color = initialData.lightColor.cpy(),
                direction = initialData.direction,
                initialIntensity = initialData.shaderIntensity,
                elevation = initialData.elevation,
                isManaged = false,
                isStatic = true,
                rays = 8192,
            )
    }

    private fun applyLightingData(
        timeData: TimeOfDayData,
        weatherFactors: Pair<Float, Color>,
    ) {
        val (shadowMultiplier, lightMultiplier) = weatherFactors

        val finalSunElevation = if (DYNAMIC_TIME_OF_DAY) timeData.elevation else currentParams.sunElevation
        sunLight?.direction = timeData.direction
        sunLight?.shaderLight?.elevation = finalSunElevation

        sunLight?.shaderIntensity = timeData.shaderIntensity * currentParams.shaderIntensity

        val newR = timeData.lightColor.r * currentParams.directionalLightIntensity
        val newG = timeData.lightColor.g * currentParams.directionalLightIntensity
        val newB = timeData.lightColor.b * currentParams.directionalLightIntensity
        val newA = timeData.lightColor.a * shadowMultiplier

        sunLight?.color = Color(newR, newG, newB, newA)

        lightEngine.setNormalInfluence(currentParams.normalInfluence)
        lightEngine.setSpecularIntensity(currentParams.specularIntensity)

        val baseAmbient = timeData.ambientColor.cpy() * lightMultiplier

        val shaderAmbient = baseAmbient.cpy()
        shaderAmbient.r *= currentParams.shaderAmbientStrength
        shaderAmbient.g *= currentParams.shaderAmbientStrength
        shaderAmbient.b *= currentParams.shaderAmbientStrength
        lightEngine.updateShaderAmbientColor(shaderAmbient)

        val box2dAmbient = baseAmbient.cpy()
        box2dAmbient.r *= currentParams.box2dLightStrength
        box2dAmbient.g *= currentParams.box2dLightStrength
        box2dAmbient.b *= currentParams.box2dLightStrength
        lightEngine.setBox2dAmbientLight(box2dAmbient)
    }
}
