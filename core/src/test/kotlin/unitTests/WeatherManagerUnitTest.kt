package unitTests

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.math.MathUtils
import io.bennyoe.components.Weather
import io.bennyoe.systems.light.WeatherManager
import io.bennyoe.utility.interpolateColor
import org.junit.jupiter.api.Test
import kotlin.math.abs

class WeatherManagerUnitTest {
    // --- helpers -------------------------------------------------------------

    private fun assertFloatClose(
        expected: Float,
        actual: Float,
        eps: Float = 1e-4f,
        msg: String = "float",
    ) {
        if (abs(expected - actual) > eps) {
            kotlin.test.fail("$msg: expected=$expected actual=$actual eps=$eps")
        }
    }

    private fun assertColorClose(
        expected: Color,
        actual: Color,
        eps: Float = 1e-4f,
        prefix: String = "color",
    ) {
        assertFloatClose(expected.r, actual.r, eps, "$prefix.r")
        assertFloatClose(expected.g, actual.g, eps, "$prefix.g")
        assertFloatClose(expected.b, actual.b, eps, "$prefix.b")
        assertFloatClose(expected.a, actual.a, eps, "$prefix.a")
    }

    // --- tests ---------------------------------------------------------------

    @Test
    fun `default factors before first update are neutral`() {
        val mgr = WeatherManager()

        val (shadow, light) = mgr.getFactors()

        assertFloatClose(1f, shadow, msg = "shadow default")
        assertColorClose(Color.WHITE, light, prefix = "light default")
    }

    @Test
    fun `steady state no transition returns current weather multipliers`() {
        val mgr = WeatherManager()
        val current = Weather.CLEAR // choose any stable weather in your enum

        // first call just sets previousWeather
        mgr.update(delta = 0f, currentWeather = current)

        // ask factors multiple times (no change)
        repeat(3) { mgr.update(delta = 0.016f, currentWeather = current) }

        val (shadow, light) = mgr.getFactors()

        assertFloatClose(current.shadowMultiplier, shadow, msg = "shadow steady")
        assertColorClose(current.lightMultiplier, light, prefix = "light steady")
    }

    @Test
    fun `transition uses target duration when previous is not RAIN and interpolates with pow2`() {
        val mgr = WeatherManager()
        val from = Weather.CLEAR
        val to =
            if (Weather.entries.any { it != from && it != Weather.RAIN }) {
                Weather.entries.first { it != from && it != Weather.RAIN }
            } else {
                // fallback: if only CLEAR and RAIN exist, use RAIN as target (still valid for this test)
                Weather.RAIN
            }

        // initialize previous = from
        mgr.update(delta = 0f, currentWeather = from)

        // start transition to 'to' (elapsed stays 0 because delta=0)
        mgr.update(delta = 0f, currentWeather = to)

        // advance half of the chosen duration (target duration because prev != RAIN)
        val dur = to.transitionDuration.coerceAtLeast(0f)
        val half = if (dur > 0f) dur * 0.5f else 0f
        mgr.update(delta = half, currentWeather = to)

        val (shadow, light) = mgr.getFactors()

        val t = if (dur <= 0f) 1f else (half / dur).coerceIn(0f, 1f) // should be 0.5 unless dur==0
        val te = Interpolation.pow2.apply(t)

        val expectedShadow = MathUtils.lerp(from.shadowMultiplier, to.shadowMultiplier, te)
        val expectedLight = interpolateColor(from.lightMultiplier, to.lightMultiplier, te)

        assertFloatClose(expectedShadow, shadow, msg = "shadow mid transition (target duration)")
        assertColorClose(expectedLight, light, prefix = "light mid transition (target duration)")
    }

    @Test
    fun `transition uses previous RAIN duration when coming from RAIN`() {
        val mgr = WeatherManager()
        val from = Weather.RAIN
        val to = Weather.entries.first { it != Weather.RAIN } // any non-RAIN target

        // initialize previous = RAIN
        mgr.update(delta = 0f, currentWeather = from)

        // start transition to 'to'
        mgr.update(delta = 0f, currentWeather = to)

        // advance half of the PREVIOUS (RAIN) duration
        val dur = from.transitionDuration.coerceAtLeast(0f)
        val half = if (dur > 0f) dur * 0.5f else 0f
        mgr.update(delta = half, currentWeather = to)

        val (shadow, light) = mgr.getFactors()

        val t = if (dur <= 0f) 1f else (half / dur).coerceIn(0f, 1f)
        val te = Interpolation.pow2.apply(t)

        val expectedShadow = MathUtils.lerp(from.shadowMultiplier, to.shadowMultiplier, te)
        val expectedLight = interpolateColor(from.lightMultiplier, to.lightMultiplier, te)

        assertFloatClose(expectedShadow, shadow, msg = "shadow mid transition (prev=RAIN)")
        assertColorClose(expectedLight, light, prefix = "light mid transition (prev=RAIN)")
    }

    @Test
    fun `transition completes at or beyond duration and clears state`() {
        val mgr = WeatherManager()
        val from = Weather.CLEAR
        val to = Weather.RAIN // pick something different

        // initialize previous = from
        mgr.update(delta = 0f, currentWeather = from)

        // start transition to 'to'
        mgr.update(delta = 0f, currentWeather = to)

        // advance by full chosen duration (since prev != RAIN, use target)
        val dur = to.transitionDuration.coerceAtLeast(0f)
        val full = if (dur > 0f) dur else 0f
        mgr.update(delta = full, currentWeather = to)

        // one more small tick to ensure >= 1 clears the transition
        mgr.update(delta = 0.001f, currentWeather = to)

        val (shadow, light) = mgr.getFactors()

        assertFloatClose(to.shadowMultiplier, shadow, msg = "shadow after completion")
        assertColorClose(to.lightMultiplier, light, prefix = "light after completion")
    }
}
