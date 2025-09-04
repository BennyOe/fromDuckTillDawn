package unitTests

import io.bennyoe.utility.angleToHour
import io.bennyoe.utility.hourToAngle
import io.bennyoe.utility.isHourIn
import io.bennyoe.utility.nightFactor
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class TimeUtilityUnitTest {
    @Test
    fun `converts angle to hour and back`() {
        val hour = 12f
        val angle = hourToAngle(hour)
        val convertedHour = angleToHour(angle)

        assertEquals(hour, convertedHour)
    }

    @Test
    fun `returns true if hour is within range`() {
        // Arrange
        val startHour = 9f
        val endHour = 17f
        val hourInRange = 12f
        val hourOutOfRange = 18f

        val overnightStart = 22f
        val overnightEnd = 14f
        val overnightHourInRange = 7f
        val overnightHourOutOfRange = 17f

        // Act
        val resultInRange = isHourIn(hourInRange, startHour, endHour)
        val resultOutOfRange = isHourIn(hourOutOfRange, startHour, endHour)
        val resultOvernightInRange = isHourIn(overnightHourInRange, overnightStart, overnightEnd)
        val resultOvernightOutOfRange = isHourIn(overnightHourOutOfRange, overnightStart, overnightEnd)

        // Assert
        assertEquals(true, resultInRange)
        assertEquals(false, resultOutOfRange)

        assertEquals(true, resultOvernightInRange)
        assertEquals(false, resultOvernightOutOfRange)
    }

    @Test
    fun `returns 0,5 when time is 12`() {
        val sunset = 19f
        val sunrise = 5f
        val resultSunset = nightFactor(sunset)
        val resultSunrise = nightFactor(sunrise)
        assertEquals(0.5f, resultSunset)
        assertEquals(0.5f, resultSunrise)
    }

    @Test
    fun `returns 1 when it is night`() {
        val time = 23f
        val result = nightFactor(time)
        assertEquals(1f, result)
    }

    @Test
    fun `returns 0 when it is day`() {
        val time = 14f
        val result = nightFactor(time)
        assertEquals(0f, result)
    }
}
