package io.bennyoe.systems.light

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.github.quillraven.fleks.IntervalSystem
import com.github.quillraven.fleks.World.Companion.inject
import io.bennyoe.components.GameStateComponent
import io.bennyoe.config.GameConstants.INITIAL_TIME_OF_DAY
import io.bennyoe.event.MapChangedEvent
import io.bennyoe.lightEngine.core.GameLight
import io.bennyoe.lightEngine.core.Scene2dLightEngine
import io.bennyoe.utility.times

class AmbientLightSystem(
    private val lightEngine: Scene2dLightEngine = inject("lightEngine"),
) : IntervalSystem(),
    EventListener {
    private val gameStateCmp by lazy { world.family { all(GameStateComponent) }.first()[GameStateComponent] }
    private val timeOfDayManager: TimeOfDayManager = TimeOfDayManager()
    private val weatherManager: WeatherManager = WeatherManager()
    private var sunLight: GameLight.Directional? = null

    override fun onTick() {
        weatherManager.update(deltaTime, gameStateCmp.weather)
        val timeProps = timeOfDayManager.getProperties(gameStateCmp.timeOfDay)
        val weatherFactors = weatherManager.getFactors()

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
        lightEngine.setSpecularIntensity(.4f)
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

        sunLight?.direction = timeData.direction
        sunLight?.shaderLight?.elevation = timeData.elevation
        sunLight?.shaderIntensity = timeData.shaderIntensity
        sunLight?.color =
            Color(
                timeData.lightColor.r,
                timeData.lightColor.g,
                timeData.lightColor.b,
                timeData.lightColor.a * shadowMultiplier,
            )

        lightEngine.setNormalInfluence(timeData.normalInfluence)
        lightEngine.setBox2dAmbientLight(timeData.ambientColor * lightMultiplier)
        lightEngine.updateShaderAmbientColor(timeData.ambientColor * lightMultiplier)
    }
}
