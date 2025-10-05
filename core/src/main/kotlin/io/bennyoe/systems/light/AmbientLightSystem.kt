package io.bennyoe.systems.light

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.github.quillraven.fleks.IntervalSystem
import com.github.quillraven.fleks.World.Companion.inject
import io.bennyoe.components.GameStateComponent
import io.bennyoe.components.debug.DebugComponent
import io.bennyoe.config.GameConstants.DYNAMIC_TIME_OF_DAY
import io.bennyoe.config.GameConstants.INITIAL_TIME_OF_DAY
import io.bennyoe.event.MapChangedEvent
import io.bennyoe.lightEngine.core.GameLight
import io.bennyoe.lightEngine.core.Scene2dLightEngine

class AmbientLightSystem(
    private val lightEngine: Scene2dLightEngine = inject("lightEngine"),
) : IntervalSystem(),
    EventListener {
    private val gameStateCmp by lazy { world.family { all(GameStateComponent) }.first()[GameStateComponent] }
    private val debugCmp by lazy { world.family { all(DebugComponent) }.firstOrNull()?.getOrNull(DebugComponent) }
    private val timeOfDayManager: TimeOfDayManager = TimeOfDayManager()
    private val weatherManager: WeatherManager = WeatherManager()
    private var sunLight: GameLight.Directional? = null

    override fun onTick() {
        weatherManager.update(deltaTime, gameStateCmp.weather)
        val timeProps = timeOfDayManager.getProperties(gameStateCmp.timeOfDay)
        val weatherFactors = weatherManager.getFactors()

        debugCmp?.let {
            lightEngine.setDiffuseLight(it.useDiffuseLight)
        }

        applyLightingData(timeProps, weatherFactors)
    }

    override fun handle(event: Event?): Boolean {
        if (event is MapChangedEvent) {
            setupLights()
            return true
        }
        return false
    }

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

        val debugShaderIntensity = debugCmp?.shaderIntensity ?: 1f
        val debugDirectionalLightIntensity = debugCmp?.directionalLightIntensity ?: 1f
        val debugBox2dLightStrength = debugCmp?.box2dLightStrength ?: 1f
        val debugNormalInfluence = debugCmp?.normalInfluence ?: timeData.normalInfluence
        val debugSpecularIntensity = debugCmp?.specularIntensity ?: 1f
        val finalSunElevation = if (DYNAMIC_TIME_OF_DAY) timeData.elevation else debugCmp?.sunElevation ?: timeData.elevation

        sunLight?.direction = timeData.direction
        sunLight?.shaderLight?.elevation = finalSunElevation
        sunLight?.shaderIntensity = timeData.shaderIntensity * debugShaderIntensity

        val newR = timeData.lightColor.r * debugDirectionalLightIntensity
        val newG = timeData.lightColor.g * debugDirectionalLightIntensity
        val newB = timeData.lightColor.b * debugDirectionalLightIntensity
        val newA = timeData.lightColor.a * shadowMultiplier

        sunLight?.color = Color(newR, newG, newB, newA)

        // Apply Normal Influence and Specular Intensity from sliders
        lightEngine.setNormalInfluence(debugNormalInfluence)
        lightEngine.setSpecularIntensity(debugSpecularIntensity)

        val finalAmbient = timeData.ambientColor.cpy().mul(lightMultiplier)
        finalAmbient.a *= debugBox2dLightStrength // Apply Box2D strength to alpha
        lightEngine.setBox2dAmbientLight(finalAmbient)
        lightEngine.updateShaderAmbientColor(finalAmbient)
    }
}
