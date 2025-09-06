package io.bennyoe.utility

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.MathUtils

/**
 * Interpolates between two colors.
 */
fun interpolateColor(
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

/**
 * Multiplies the RGB components of two colors.
 */
operator fun Color.times(s: Color): Color = Color(r * s.r, g * s.g, b * s.b, a).clamp()
