package io.bennyoe.systems.light

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.MathUtils
import io.bennyoe.components.Weather
import io.bennyoe.utility.interpolateColor

class WeatherManager {
    private var previousWeather: Weather? = null
    private var weatherTx: WeatherTransition? = null

    /**
     * Updates the weather transition state. Call this once per frame.
     */
    fun update(
        delta: Float,
        currentWeather: Weather,
    ) {
        if (previousWeather == null) {
            previousWeather = currentWeather
            return
        }
        val prev = previousWeather!!

        if (currentWeather != prev) {
            val duration = currentWeather.transitionDuration.coerceAtLeast(0f)
            weatherTx = WeatherTransition(from = prev, to = currentWeather, duration = duration)
            previousWeather = currentWeather
        }

        weatherTx?.let {
            it.elapsed += delta
            if (it.transition() >= 1f) {
                weatherTx = null
            }
        }
    }

    /**
     * Gets the current interpolated weather factors (shadow and light multipliers).
     */
    fun getFactors(): Pair<Float, Color> {
        val currentWeather = previousWeather ?: return 1f to Color.WHITE

        val t = weatherTx?.transition() ?: 1f

        val (shadowFrom, shadowTo) =
            if (weatherTx != null) {
                weatherTx!!.from.shadowMultiplier to weatherTx!!.to.shadowMultiplier
            } else {
                currentWeather.shadowMultiplier to currentWeather.shadowMultiplier
            }

        val (lightFrom, lightTo) =
            if (weatherTx != null) {
                weatherTx!!.from.lightMultiplier to weatherTx!!.to.lightMultiplier
            } else {
                currentWeather.lightMultiplier to currentWeather.lightMultiplier
            }

        // Using smoothstep for a nicer transition
        val te = t * t * (3f - 2f * t)

        val shadow = MathUtils.lerp(shadowFrom, shadowTo, te)
        val light = interpolateColor(lightFrom, lightTo, te)
        return shadow to light
    }

    private data class WeatherTransition(
        val from: Weather,
        val to: Weather,
        var elapsed: Float = 0f,
        val duration: Float,
    ) {
        fun transition(): Float = if (duration <= 0f) 1f else (elapsed / duration).coerceIn(0f, 1f)
    }
}
