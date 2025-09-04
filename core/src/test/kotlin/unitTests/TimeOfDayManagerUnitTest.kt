package unitTests

import com.badlogic.gdx.graphics.Color
import io.bennyoe.components.TimeOfDay
import io.bennyoe.systems.light.TimeOfDayData
import io.bennyoe.systems.light.TimeOfDayManager
import org.junit.jupiter.api.Test
import kotlin.math.abs
import kotlin.test.assertEquals

class TimeOfDayManagerUnitTest {
    private fun assertFloatClose(
        expected: Float,
        actual: Float,
        eps: Float = 1e-3f,
        msg: String? = null,
    ) {
        if (abs(expected - actual) > eps) {
            kotlin.test.fail((msg ?: "Floats differ") + ": expected=$expected, actual=$actual, eps=$eps")
        }
    }

    private fun assertColorClose(
        expected: Color,
        actual: Color,
        eps: Float = 1e-3f,
        prefix: String = "",
    ) {
        assertFloatClose(expected.r, actual.r, eps, "$prefix.r")
        assertFloatClose(expected.g, actual.g, eps, "$prefix.g")
        assertFloatClose(expected.b, actual.b, eps, "$prefix.b")
        assertFloatClose(expected.a, actual.a, eps, "$prefix.a")
    }

    private fun assertTimeOfDayDataClose(
        expected: TimeOfDayData,
        actual: TimeOfDayData,
        eps: Float = 1e-3f,
    ) {
        assertEquals(expected.timeOfDay, actual.timeOfDay, "timeOfDay")
        assertColorClose(expected.lightColor, actual.lightColor, eps, "lightColor")
        assertColorClose(expected.ambientColor, actual.ambientColor, eps, "ambientColor")
        assertFloatClose(expected.direction, actual.direction, eps, "direction")
        assertFloatClose(expected.shaderIntensity, actual.shaderIntensity, eps, "shaderIntensity")
        assertFloatClose(expected.elevation, actual.elevation, eps, "elevation")
        assertFloatClose(expected.normalInfluence, actual.normalInfluence, eps, "normalInfluence")
        assertColorClose(expected.tintColor, actual.tintColor, eps, "tintColor")
    }

    // --- Existing test kept as-is -------------------------------------------
    @Test
    fun `get day data when time is during day`() {
        val timeOfDayData =
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
        val time = 12f
        val result = TimeOfDayManager().getProperties(time)
        assertEquals(timeOfDayData, result)
    }

    // --- Boundary tests ------------------------------------------------------

    @Test
    fun `at 6_00 we are TWILIGHT (end of night-twilight), twilight values, direction 0`() {
        val time = 6f
        val expected =
            TimeOfDayData(
                timeOfDay = TimeOfDay.TWILIGHT,
                lightColor = Color(0.3f, 0.25f, 0.4f, 0f),
                ambientColor = Color(0.25f, 0.22f, 0.35f, 1f),
                direction = 0f,
                shaderIntensity = 0.5f,
                elevation = 15f,
                normalInfluence = 0.7f,
                tintColor = Color(0.2f, 0.15f, 0.3f, 0.6f),
            )
        val result = TimeOfDayManager().getProperties(time)
        assertTimeOfDayDataClose(expected, result)
    }

    @Test
    fun `at 7_00 we are DAWN (end of twilight-dawn), dawn values, direction 15`() {
        val time = 7f
        val expectedDirection = ((time - 6f) / 12f) * 180f // 15°
        val expected =
            TimeOfDayData(
                timeOfDay = TimeOfDay.DAWN,
                lightColor = Color(0.8f, 0.6f, 0.4f, 0.8f),
                ambientColor = Color(0.4f, 0.3f, 0.2f, 1f),
                direction = expectedDirection,
                shaderIntensity = 2.5f,
                elevation = 25f,
                normalInfluence = 0.8f,
                tintColor = Color(0.8f, 0.5f, 0.3f, 0.7f),
            )
        val result = TimeOfDayManager().getProperties(time)
        assertTimeOfDayDataClose(expected, result)
    }

    @Test
    fun `at 9_00 we are in stable DAY segment (no interpolation)`() {
        val time = 9f // 9..16 -> dayData, no interpolation
        val expectedDirection = ((time - 6f) / 12f) * 180f // = 45 deg
        val expected =
            TimeOfDayData(
                timeOfDay = TimeOfDay.DAY,
                lightColor = Color(0.75f, 0.6f, 0.8f, 1f),
                ambientColor = Color(0.4f, 0.5f, 0.7f, 1f),
                direction = expectedDirection,
                shaderIntensity = 4.5f,
                elevation = 60f,
                normalInfluence = 0.9f,
                tintColor = Color(0.95f, 0.95f, 0.9f, 0.8f),
            )
        val result = TimeOfDayManager().getProperties(time)
        assertTimeOfDayDataClose(expected, result)
    }

    @Test
    fun `at 16_00 we are still DAY (stable), day values, direction 150`() {
        val time = 16f // still in 9..16 stable day
        val expectedDirection = ((time - 6f) / 12f) * 180f // 150°
        val expected =
            TimeOfDayData(
                timeOfDay = TimeOfDay.DAY,
                lightColor = Color(0.75f, 0.6f, 0.8f, 1f),
                ambientColor = Color(0.4f, 0.5f, 0.7f, 1f),
                direction = expectedDirection,
                shaderIntensity = 4.5f,
                elevation = 60f,
                normalInfluence = 0.9f,
                tintColor = Color(0.95f, 0.95f, 0.9f, 0.8f),
            )
        val result = TimeOfDayManager().getProperties(time)
        assertTimeOfDayDataClose(expected, result)
    }

    @Test
    fun `at 18_00 we are TWILIGHT (end of dusk-twilight2), twilight2 values, direction 180`() {
        val time = 18f // 18.00 is still in 6..18 for direction (inclusive range), so direction = 180°
        val expectedDirection = ((time - 6f) / 12f) * 180f // 180°

        val expected =
            TimeOfDayData(
                timeOfDay = TimeOfDay.TWILIGHT,
                lightColor = Color(0.4f, 0.2f, 0.1f, 0f),
                ambientColor = Color(0.3f, 0.2f, 0.3f, 1f),
                direction = expectedDirection,
                shaderIntensity = 0.3f,
                elevation = 10f,
                normalInfluence = 0.6f,
                tintColor = Color(0.3f, 0.15f, 0.08f, 0.5f),
            )

        val result = TimeOfDayManager().getProperties(time)
        assertTimeOfDayDataClose(expected, result)
    }

    // --- Interpolation (midpoints) ------------------------------------------

    @Test
    fun `at 8_00 we are halfway DAWN to DAY (t=0_5) and direction 30`() {
        val time = 8f // in 7..9, factor = (8-7)/2 = 0.5
        val t = 0.5f
        val expectedDirection = ((time - 6f) / 12f) * 180f // = 30 deg

        fun lerp(
            a: Float,
            b: Float,
        ) = a + (b - a) * t

        fun cl(
            r1: Float,
            g1: Float,
            b1: Float,
            a1: Float,
            r2: Float,
            g2: Float,
            b2: Float,
            a2: Float,
        ) = Color(lerp(r1, r2), lerp(g1, g2), lerp(b1, b2), lerp(a1, a2))

        val dawn =
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
        val day =
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

        val expected =
            TimeOfDayData(
                timeOfDay = TimeOfDay.DAY,
                lightColor =
                    cl(
                        dawn.lightColor.r,
                        dawn.lightColor.g,
                        dawn.lightColor.b,
                        dawn.lightColor.a,
                        day.lightColor.r,
                        day.lightColor.g,
                        day.lightColor.b,
                        day.lightColor.a,
                    ),
                ambientColor =
                    cl(
                        dawn.ambientColor.r,
                        dawn.ambientColor.g,
                        dawn.ambientColor.b,
                        dawn.ambientColor.a,
                        day.ambientColor.r,
                        day.ambientColor.g,
                        day.ambientColor.b,
                        day.ambientColor.a,
                    ),
                direction = expectedDirection,
                shaderIntensity = lerp(dawn.shaderIntensity, day.shaderIntensity),
                elevation = lerp(dawn.elevation, day.elevation),
                normalInfluence = lerp(dawn.normalInfluence, day.normalInfluence),
                tintColor =
                    cl(
                        dawn.tintColor.r,
                        dawn.tintColor.g,
                        dawn.tintColor.b,
                        dawn.tintColor.a,
                        day.tintColor.r,
                        day.tintColor.g,
                        day.tintColor.b,
                        day.tintColor.a,
                    ),
            )

        val result = TimeOfDayManager().getProperties(time)
        assertTimeOfDayDataClose(expected, result, eps = 1e-3f)
    }

    @Test
    fun `at 17_5 we are halfway DUSK to TWILIGHT2 and direction about 172_5`() {
        val time = 17.5f // 17..18 -> dusk->twilight2, t=0.5
        val t = 0.5f
        val expectedDirection = ((time - 6f) / 12f) * 180f // ≈ 172.5

        fun lerp(
            a: Float,
            b: Float,
        ) = a + (b - a) * t

        fun cl(
            r1: Float,
            g1: Float,
            b1: Float,
            a1: Float,
            r2: Float,
            g2: Float,
            b2: Float,
            a2: Float,
        ) = Color(lerp(r1, r2), lerp(g1, g2), lerp(b1, b2), lerp(a1, a2))

        val dusk =
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
        val twilight2 =
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

        val expected =
            TimeOfDayData(
                timeOfDay = TimeOfDay.TWILIGHT,
                lightColor =
                    cl(
                        dusk.lightColor.r,
                        dusk.lightColor.g,
                        dusk.lightColor.b,
                        dusk.lightColor.a,
                        twilight2.lightColor.r,
                        twilight2.lightColor.g,
                        twilight2.lightColor.b,
                        twilight2.lightColor.a,
                    ),
                ambientColor =
                    cl(
                        dusk.ambientColor.r,
                        dusk.ambientColor.g,
                        dusk.ambientColor.b,
                        dusk.ambientColor.a,
                        twilight2.ambientColor.r,
                        twilight2.ambientColor.g,
                        twilight2.ambientColor.b,
                        twilight2.ambientColor.a,
                    ),
                direction = expectedDirection,
                shaderIntensity = lerp(dusk.shaderIntensity, twilight2.shaderIntensity),
                elevation = lerp(dusk.elevation, twilight2.elevation),
                normalInfluence = lerp(dusk.normalInfluence, twilight2.normalInfluence),
                tintColor =
                    cl(
                        dusk.tintColor.r,
                        dusk.tintColor.g,
                        dusk.tintColor.b,
                        dusk.tintColor.a,
                        twilight2.tintColor.r,
                        twilight2.tintColor.g,
                        twilight2.tintColor.b,
                        twilight2.tintColor.a,
                    ),
            )

        val result = TimeOfDayManager().getProperties(time)
        assertTimeOfDayDataClose(expected, result, eps = 1e-3f)
    }

    // --- Night-time checks ---------------------------------------------------

    @Test
    fun `at 2_00 night values with direction 120`() {
        val time = 2f // "else" branch (0..6): lerp 90..180
        val expectedDirection = 90f + (time / 6f) * (180f - 90f) // = 120

        val expected =
            TimeOfDayData(
                timeOfDay = TimeOfDay.NIGHT,
                lightColor = Color(0.3f, 0.3f, 0.6f, 1f),
                ambientColor = Color(0.05f, 0.13f, 0.45f, 1f),
                direction = expectedDirection,
                shaderIntensity = 5f,
                elevation = 5f,
                normalInfluence = 1f,
                tintColor = Color(0.1f, 0.18f, 0.25f, .4f),
            )

        val result = TimeOfDayManager().getProperties(time)
        assertTimeOfDayDataClose(expected, result)
    }

    @Test
    fun `at 23_00 twilight2 to night segment has correct direction`() {
        val time = 23f // 18..24 -> direction 0..90
        val expectedDirection = ((time - 18f) / 6f) * 90f // ≈ 75

        // We don't assert all properties here—just the direction and timeOfDay are enough for this check.
        val result = TimeOfDayManager().getProperties(time)
        assertFloatClose(expectedDirection, result.direction, 1e-3f, "direction@23")
        // time period should be NIGHT or TWILIGHT depending on interpolation window outside 18..19; at 23 it's stable NIGHT
        assertEquals(TimeOfDay.NIGHT, result.timeOfDay)
    }

    @Test
    fun `at 0_00 direction is 90`() {
        val time = 0f // start of 0..6 branch -> 90 deg
        val result = TimeOfDayManager().getProperties(time)
        assertFloatClose(90f, result.direction, 1e-3f, "direction@0")
        assertEquals(TimeOfDay.NIGHT, result.timeOfDay)
    }

    // --- A quick stable-day check at 10_00 ----------------------------------

    @Test
    fun `at 10_00 stable day values and direction 60`() {
        val time = 10f
        val expectedDirection = ((time - 6f) / 12f) * 180f // = 60

        val expected =
            TimeOfDayData(
                timeOfDay = TimeOfDay.DAY,
                lightColor = Color(0.75f, 0.6f, 0.8f, 1f),
                ambientColor = Color(0.4f, 0.5f, 0.7f, 1f),
                direction = expectedDirection,
                shaderIntensity = 4.5f,
                elevation = 60f,
                normalInfluence = 0.9f,
                tintColor = Color(0.95f, 0.95f, 0.9f, 0.8f),
            )

        val result = TimeOfDayManager().getProperties(time)
        assertTimeOfDayDataClose(expected, result)
    }
}
