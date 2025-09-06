package io.bennyoe.utility

import kotlin.math.PI

private const val HOURS_IN_DAY = 24f
private const val TWO_PI = (PI * 2).toFloat()
private const val NINETY_DEGREE_OFFSET_RADIANS = (PI / 2).toFloat()

/** Keep angle in [0, 2π) */
private fun normalizeRadians(a: Float): Float {
    var x = a % TWO_PI
    if (x < 0f) x += TWO_PI
    return x
}

/** Converts hours (0..24) to angle in radians (0..2π) */
fun hourToAngle(hours: Float): Float {
    val raw = (hours / HOURS_IN_DAY) * TWO_PI - NINETY_DEGREE_OFFSET_RADIANS
    return normalizeRadians(raw)
}

/** Converts angle in radians (0..2π) to hours (0..24) */
fun angleToHour(angle: Float): Float {
    val normalized = normalizeRadians(angle + NINETY_DEGREE_OFFSET_RADIANS)
    return (normalized / TWO_PI) * HOURS_IN_DAY
}

/**
 * Checks if a given hour falls within a time range, correctly handling overnight periods.
 * e.g., isHourIn(23f, 22f, 4f) returns true.
 */
fun isHourIn(
    hour: Float,
    start: Float,
    end: Float,
): Boolean =
    if (start <= end) {
        hour in start..end
    } else {
        hour >= start || hour <= end
    }

/**
 * Calculates a factor from 0.0 (full day) to 1.0 (full night)
 * with smooth transitions for sunrise and sunset.
 */
fun nightFactor(hour: Float): Float {
    fun ramp(
        x: Float,
        a: Float,
        b: Float,
    ): Float = ((x - a) / (b - a)).coerceIn(0f, 1f)

    val sunset = ramp(hour, 18f, 20f) // 0 -> 1 during sunset
    val sunrise = 1f - ramp(hour, 4f, 6f) // 1 -> 0 during sunrise
    val hardNight = if (isHourIn(hour, 20f, 4f)) 1f else 0f // 1 if it's deep night
    return maxOf(hardNight, sunset, sunrise)
}
