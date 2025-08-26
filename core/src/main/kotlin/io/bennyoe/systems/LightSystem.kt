package io.bennyoe.systems

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import com.github.quillraven.fleks.World.Companion.inject
import io.bennyoe.components.GameStateComponent
import io.bennyoe.components.LightComponent
import io.bennyoe.components.PhysicComponent
import io.bennyoe.components.TimeOfDay
import io.bennyoe.components.TransformComponent
import io.bennyoe.event.MapChangedEvent
import io.bennyoe.lightEngine.core.GameLight
import io.bennyoe.lightEngine.core.Scene2dLightEngine
import ktx.log.logger
import ktx.math.vec2

// TODO refactor
class LightSystem(
    private val lightEngine: Scene2dLightEngine = inject("lightEngine"),
) : IteratingSystem(family { all(LightComponent, TransformComponent) }),
    EventListener {
    private val gameStateCmp by lazy { world.family { all(GameStateComponent) }.first()[GameStateComponent] }
    private var sunLight: GameLight.Directional? = null

    override fun onTick() {
        updateDaylightFromTimeOfDay()
        super.onTick()
    }

    override fun onTickEntity(entity: Entity) {
        val lightCmp = entity[LightComponent]
        val transformCmp = entity[TransformComponent]
        // with PhysicComponent the center is in the middle of the image
        if (entity has PhysicComponent) {
            when (val light = lightCmp.gameLight) {
                is GameLight.Spot -> light.position = transformCmp.position
                is GameLight.Point -> light.position = transformCmp.position
                else -> {}
            }
        }
        // without PhysicComponent the center is in the bottom left of the image
        if (entity hasNo PhysicComponent) {
            val newPos = vec2(transformCmp.position.x + transformCmp.width / 2, transformCmp.position.y + transformCmp.height / 2)
            when (val light = lightCmp.gameLight) {
                is GameLight.Spot -> light.position = newPos
                is GameLight.Point -> light.position = newPos
                else -> {}
            }
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

    // 0° = Light from right
    // 90° = Light from top
    // 180° = Light from left
    // 270° = Light from bottom

    // Times: 5-6 Twilight, 6-8 Dawn, 8-16 Day, 16-18 Dusk, 18-19 Twilight2, 19-5 Night

    private val twilightData =
        TimeOfDayData(
            timeOfDay = TimeOfDay.TWILIGHT,
            lightColor = Color(0.3f, 0.25f, 0.4f, 0f),
            ambientColor = Color(0.25f, 0.22f, 0.35f, 1f),
            direction = 90f,
            shaderIntensity = 0.5f,
            elevation = 15f,
            normalInfluence = 0.7f,
            tintColor = Color(0.2f, 0.15f, 0.3f, 0.6f),
        )

    private val dawnData =
        TimeOfDayData(
            timeOfDay = TimeOfDay.DAWN,
            lightColor = Color(0.8f, 0.6f, 0.4f, 0.8f),
            ambientColor = Color(0.4f, 0.3f, 0.2f, 1f),
            direction = 45f,
            shaderIntensity = 2.5f,
            elevation = 25f,
            normalInfluence = 0.8f,
            tintColor = Color(0.8f, 0.5f, 0.3f, 0.7f),
        )

    private val dayData =
        TimeOfDayData(
            timeOfDay = TimeOfDay.DAY,
            lightColor = Color(0.75f, 0.6f, 0.8f, 1f),
            ambientColor = Color(0.4f, 0.5f, 0.7f, 1f),
            direction = 90f,
            shaderIntensity = 4.5f,
            elevation = 60f,
            normalInfluence = 0.9f,
            tintColor = Color(0.95f, 0.95f, 0.9f, 0.8f),
        )

    private val duskData =
        TimeOfDayData(
            timeOfDay = TimeOfDay.DUSK,
            lightColor = Color(0.8f, 0.5f, 0.3f, 1f),
            ambientColor = Color(0.5f, 0.3f, 0.15f, 1f),
            direction = 135f,
            shaderIntensity = 2.0f,
            elevation = 20f,
            normalInfluence = 0.8f,
            tintColor = Color(0.9f, 0.4f, 0.2f, 0.7f),
        )

    private val twilight2Data =
        TimeOfDayData(
            timeOfDay = TimeOfDay.TWILIGHT,
            lightColor = Color(0.4f, 0.2f, 0.1f, 0f),
            ambientColor = Color(0.3f, 0.2f, 0.3f, 1f),
            direction = 180f,
            shaderIntensity = 0.3f,
            elevation = 10f,
            normalInfluence = 0.6f,
            tintColor = Color(0.3f, 0.15f, 0.08f, 0.5f),
        )

    private val nightData =
        TimeOfDayData(
            TimeOfDay.NIGHT,
            Color(0.3f, 0.3f, 0.6f, 1f),
            Color(0.05f, 0.13f, 0.45f, 1f),
            135f,
            5f,
            5f,
            1f,
            Color(0.1f, 0.18f, 0.25f, .4f),
        )

    private fun updateDaylightFromTimeOfDay() {
        val time = gameStateCmp.timeOfDay // 0.0 .. 24.0

        // Get current time period data and potential transition data
        val (currentData, nextData, transitionFactor) = getCurrentTimeOfDayData(time)

        // If we're in a transition period, interpolate between the two data sets
        val finalData =
            if (nextData != null && transitionFactor > 0f) {
                interpolateTimeOfDayData(currentData, nextData, transitionFactor)
            } else {
                currentData
            }

        val finalDirection =
            when (time) {
                in 6f..18f -> {
                    val factor = (time - 6f) / 12f
                    MathUtils.lerp(0f, 180f, factor)
                }

                in 18f..24f -> {
                    val factor = (time - 18f) / 6f
                    MathUtils.lerp(0f, 90f, factor)
                }

                else -> {
                    val factor = (time) / 6f
                    MathUtils.lerp(90f, 180f, factor)
                }
            }

        // Apply the calculated lighting data
        applyLightingData(
            lightColor = finalData.lightColor,
            direction = finalDirection,
            shaderIntensity = finalData.shaderIntensity,
            elevation = finalData.elevation,
            normalInfluence = finalData.normalInfluence,
            ambientColor = finalData.ambientColor,
        )
    }

    private fun getCurrentTimeOfDayData(time: Float): Triple<TimeOfDayData, TimeOfDayData?, Float> =
        when (time) {
            in 5f..6f -> {
                val factor = (time - 5f) / 1f
                Triple(nightData, twilightData, factor)
            }

            in 6f..7f -> {
                val factor = (time - 6f) / 1f
                Triple(twilightData, dawnData, factor)
            }

            in 7f..9f -> {
                val factor = (time - 7f) / 2f
                Triple(dawnData, dayData, factor)
            }

            in 9f..16f -> {
                Triple(dayData, null, 0f)
            }

            in 16f..17f -> {
                val factor = (time - 16f) / 1f
                Triple(dayData, duskData, factor)
            }

            in 17f..18f -> {
                val factor = (time - 17f) / 1f
                Triple(duskData, twilight2Data, factor)
            }

            in 18f..19f -> {
                val factor = (time - 18f) / 1f
                Triple(twilight2Data, nightData, factor)
            }

            else -> {
                Triple(nightData, null, 0f)
            }
        }

    private fun interpolateTimeOfDayData(
        from: TimeOfDayData,
        to: TimeOfDayData,
        factor: Float,
    ): TimeOfDayData {
        val t = factor.coerceIn(0f, 1f)

        return TimeOfDayData(
            timeOfDay = to.timeOfDay, // Use target time of day
            lightColor = interpolateColor(from.lightColor, to.lightColor, t),
            ambientColor = interpolateColor(from.ambientColor, to.ambientColor, t),
            direction = 0f, // is set later
            shaderIntensity = MathUtils.lerp(from.shaderIntensity, to.shaderIntensity, t),
            elevation = MathUtils.lerp(from.elevation, to.elevation, t),
            normalInfluence = MathUtils.lerp(from.normalInfluence, to.normalInfluence, t),
            tintColor = interpolateColor(from.tintColor, to.tintColor, t),
        )
    }

    private fun applyLightingData(
        lightColor: Color,
        direction: Float,
        shaderIntensity: Float,
        elevation: Float,
        normalInfluence: Float,
        ambientColor: Color,
    ) {
        sunLight?.direction = direction
        sunLight?.shaderLight?.elevation = elevation
        sunLight?.shaderIntensity = shaderIntensity
        sunLight?.color = lightColor
        lightEngine.setNormalInfluence(normalInfluence)
        lightEngine.setBox2dAmbientLight(ambientColor)
        lightEngine.updateShaderAmbientColor(ambientColor)
    }

    private fun interpolateColor(
        fromColor: Color,
        toColor: Color,
        factor: Float,
    ): Color {
        val t = factor.coerceIn(0f, 1f)
        val r = MathUtils.lerp(fromColor.r, toColor.r, t)
        val g = MathUtils.lerp(fromColor.g, toColor.g, t)
        val b = MathUtils.lerp(fromColor.b, toColor.b, t)
        val a = MathUtils.lerp(fromColor.a, toColor.a, t)
        return Color(r, g, b, a)
    }

    private fun setupLights() {
        // Start at night
        val initialData = dawnData
        lightEngine.updateShaderAmbientColor(initialData.ambientColor.cpy())
        lightEngine.setBox2dAmbientLight(initialData.ambientColor.cpy())
        lightEngine.setDiffuseLight(true)
        lightEngine.setNormalInfluence(initialData.normalInfluence)
        lightEngine.setSpecularIntensity(.4f)
        lightEngine.setSpecularRemap(0.0f, 0.7f)

        sunLight =
            lightEngine.addDirectionalLight(
                initialData.lightColor.cpy(),
                initialData.direction,
                initialData.shaderIntensity,
                initialData.elevation,
                isManaged = false,
                isStatic = true,
                // TODO find sweet spot
                rays = 8192,
            )
    }

    companion object {
        val logger = logger<LightSystem>()
    }
}

enum class LightType {
    POINT_LIGHT,
    SPOT_LIGHT,
}

data class TimeOfDayData(
    val timeOfDay: TimeOfDay,
    val lightColor: Color,
    val ambientColor: Color,
    val direction: Float,
    val shaderIntensity: Float,
    val elevation: Float,
    val normalInfluence: Float,
    val tintColor: Color,
)
