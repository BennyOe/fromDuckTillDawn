package io.bennyoe.systems.light

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.MathUtils
import io.bennyoe.components.TimeOfDay
import io.bennyoe.utility.interpolateColor

class TimeOfDayManager {
    fun getProperties(time: Float): TimeOfDayData {
        // Get current time period data and potential transition data
        val (currentData, nextData, transitionFactor) = getCurrentTimeOfDayData(time)

        // If we're in a transition period, interpolate between the two data sets
        val interpolatedData =
            if (nextData != null && transitionFactor > 0f) {
                interpolateTimeOfDayData(currentData, nextData, transitionFactor)
            } else {
                currentData
            }

        // Calculate the direction based on time and add it to the final data object
        val finalDirection = calculateDirection(time)
        return interpolatedData.copy(direction = finalDirection)
    }

    private fun calculateDirection(time: Float): Float =
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

    private fun getCurrentTimeOfDayData(time: Float): Triple<TimeOfDayData, TimeOfDayData?, Float> =
        when (time) {
            in 5f..6f -> Triple(nightData, twilightData, (time - 5f) / 1f)
            in 6f..7f -> Triple(twilightData, dawnData, (time - 6f) / 1f)
            in 7f..9f -> Triple(dawnData, dayData, (time - 7f) / 2f)
            in 9f..16f -> Triple(dayData, null, 0f)
            in 16f..17f -> Triple(dayData, duskData, (time - 16f) / 1f)
            in 17f..18f -> Triple(duskData, twilight2Data, (time - 17f) / 1f)
            in 18f..19f -> Triple(twilight2Data, nightData, (time - 18f) / 1f)
            else -> Triple(nightData, null, 0f)
        }

    private fun interpolateTimeOfDayData(
        from: TimeOfDayData,
        to: TimeOfDayData,
        factor: Float,
    ): TimeOfDayData {
        val t = factor.coerceIn(0f, 1f)

        return TimeOfDayData(
            timeOfDay = to.timeOfDay,
            lightColor = interpolateColor(from.lightColor, to.lightColor, t),
            ambientColor = interpolateColor(from.ambientColor, to.ambientColor, t),
            // direction is set later
            direction = 0f,
            shaderIntensity = MathUtils.lerp(from.shaderIntensity, to.shaderIntensity, t),
            elevation = MathUtils.lerp(from.elevation, to.elevation, t),
            normalInfluence = MathUtils.lerp(from.normalInfluence, to.normalInfluence, t),
            tintColor = interpolateColor(from.tintColor, to.tintColor, t),
        )
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
            shaderIntensity = 0.8f,
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
