package io.bennyoe.systems

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import io.bennyoe.components.GameStateComponent
import io.bennyoe.components.ImageComponent
import io.bennyoe.components.LightComponent
import io.bennyoe.components.ParticleComponent
import io.bennyoe.components.SkyComponent
import io.bennyoe.components.SkyComponentType
import io.bennyoe.components.TimeOfDay
import io.bennyoe.components.TransformComponent
import io.bennyoe.config.GameConstants.DYNAMIC_TIME_OF_DAY
import io.bennyoe.config.GameConstants.INITIAL_TIME_OF_DAY
import io.bennyoe.config.GameConstants.TIME_OF_DAY_SPEED
import io.bennyoe.event.MapChangedEvent
import ktx.log.logger
import ktx.tiled.height
import ktx.tiled.width
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Manages the time of day simulation, controlling the position and appearance of celestial bodies (sun, moon).
 * The orbit is centered at the bottom-middle of the map (horizon at y=0).
 */
// TODO refactor
class TimeOfDaySystem :
    IteratingSystem(
        family {
            all(SkyComponent, TransformComponent)
            any(ImageComponent, ParticleComponent)
        },
    ),
    EventListener,
    PausableSystem {
    private val gameStateCmp by lazy { world.family { all(GameStateComponent) }.first()[GameStateComponent] }

    private var mapWidth = 0
    private var mapHeight = 0
    private var centerX = 0f
    private var centerY = 0f
    private var radius = 0f

    private var angle = hourToAngle(INITIAL_TIME_OF_DAY)

    private var startFastForwardTimeOfDay = 0f
    private var endFastForwardTimeOfDay = 0f

    override fun handle(event: Event): Boolean {
        if (event is MapChangedEvent) {
            mapWidth = event.map.width
            mapHeight = event.map.height
            centerX = mapWidth / 2f
            radius = mapWidth / 2f
            return true
        }
        return false
    }

    override fun onTickEntity(entity: Entity) {
        val skyCmp = entity[SkyComponent]
        val particleCmp = entity.getOrNull(ParticleComponent)
        val transformCmp = entity[TransformComponent]
        val lightCmp = entity.getOrNull(LightComponent)
        val imageCmp = entity.getOrNull(ImageComponent)

        handleFastForwardTrigger()
        updateAngle(deltaTime)
        updateTimeOfDay()
        updateCelestialBodyLights(skyCmp, lightCmp)
        updateSky(skyCmp, imageCmp, particleCmp)

        val bodyAngle =
            when (skyCmp.type) {
                SkyComponentType.SUN -> angle
                SkyComponentType.MOON -> angle + MOON_PHASE_SHIFT_RADIANS
                else -> 0f
            }

        updateCelestialBodyTransform(transformCmp, bodyAngle)
    }

    private fun nightFactor(hour: Float): Float {
        fun ramp(
            x: Float,
            a: Float,
            b: Float,
        ): Float = ((x - a) / (b - a)).coerceIn(0f, 1f)

        val sunset = ramp(hour, 18f, 20f) // 0 -> 1
        val sunrise = 1f - ramp(hour, 4f, 6f) // 1 -> 0
        val hardNight = if (isHourIn(hour, 20f, 4f)) 1f else 0f
        return maxOf(hardNight, sunset, sunrise)
    }

    private fun updateSky(
        skyCmp: SkyComponent,
        imageCmp: ImageComponent?,
        particleCmp: ParticleComponent?,
    ) {
        val hour = gameStateCmp.timeOfDay
        val n = nightFactor(hour)
        val img = imageCmp?.image

        when (skyCmp.type) {
            SkyComponentType.SKY -> {
                val c = img!!.color
                c.set(Color.WHITE) // base
                c.lerp(Color.DARK_GRAY, n) // move towards night by factor n
                c.a = 1f
            }

            SkyComponentType.STARS -> {
                img!!.color.a = n // fade stars with night factor
            }

            SkyComponentType.SHOOTING_STAR -> {
                if (gameStateCmp.getTimeOfDay() == TimeOfDay.NIGHT) {
                    particleCmp?.enabled = true
                } else {
                    particleCmp?.enabled = false
                }
            }

            else -> Unit
        }
    }

    private fun isHourIn(
        hour: Float,
        start: Float,
        end: Float,
    ): Boolean =
        if (start <= end) {
            hour in start..end
        } else {
            hour >= start || hour <= end
        }

    private fun updateCelestialBodyLights(
        skyCmp: SkyComponent,
        lightCmp: LightComponent?,
    ) {
        val hour = angleToHour(angle)

        val on =
            when (skyCmp.type) {
                SkyComponentType.SUN -> isHourIn(hour, 4f, 19f) // day
                SkyComponentType.MOON -> isHourIn(hour, 17f, 6f) // evening -> after midnight
                else -> false
            }

        lightCmp?.gameLight?.setOn(on)
    }

    private fun handleFastForwardTrigger() {
        if (gameStateCmp.isTriggerTimeOfDayJustPressed) {
            triggerFastForward(gameStateCmp.timeOfDay)
        }

        if (startFastForwardTimeOfDay != endFastForwardTimeOfDay &&
            isTimeProgressionComplete(startFastForwardTimeOfDay, endFastForwardTimeOfDay, gameStateCmp.timeOfDay)
        ) {
            startFastForwardTimeOfDay = 0f
            endFastForwardTimeOfDay = 0f
        }
    }

    private fun updateAngle(deltaTime: Float) {
        // DYNAMIC_TIME_OF_DAY can be set in the GameConstants
        if (startFastForwardTimeOfDay != endFastForwardTimeOfDay || DYNAMIC_TIME_OF_DAY) {
            angle += TIME_OF_DAY_SPEED * deltaTime
        }
        angle %= TWO_PI
    }

    private fun updateTimeOfDay() {
        val normalizedAngle = (angle + NINETY_DEGREE_OFFSET_RADIANS) % TWO_PI
        val timeFraction = normalizedAngle / TWO_PI
        gameStateCmp.timeOfDay = HOURS_IN_DAY * timeFraction
    }

    private fun updateCelestialBodyTransform(
        transformCmp: TransformComponent,
        bodyAngle: Float,
    ) {
        val x = centerX + cos(bodyAngle) * radius * HORIZONTAL_CIRCLE_SCALE
        val y = centerY + sin(bodyAngle) * radius * VERTICAL_CIRCLE_SCALE

        val size = getDynamicSize(y) * CELESTIAL_BODY_SIZE_MULTIPLIER
        transformCmp.position.set(x, y)
        transformCmp.width = size
        transformCmp.height = size
    }

    private fun getDynamicSize(y: Float): Float {
        val distanceToCenter = abs(y - centerY)
        val threshold = radius * sin(Math.toRadians(SIZE_SCALING_THRESHOLD_DEGREES)).toFloat()
        val normalized = ((threshold - distanceToCenter) / threshold).coerceIn(0f, 1f)
        return 1f + normalized * normalized * 2f
    }

    private fun isTimeProgressionComplete(
        start: Float,
        end: Float,
        current: Float,
    ): Boolean {
        val distance = (end - start + HOURS_IN_DAY) % HOURS_IN_DAY
        val progress = (current - start + HOURS_IN_DAY) % HOURS_IN_DAY
        return progress >= distance
    }

    private fun triggerFastForward(currentTime: Float) {
        startFastForwardTimeOfDay = currentTime
        endFastForwardTimeOfDay = (currentTime + FAST_FORWARD_DURATION_HOURS) % HOURS_IN_DAY
        gameStateCmp.isTriggerTimeOfDayJustPressed = false
    }

    companion object {
        val logger = logger<TimeOfDaySystem>()

        private const val VERTICAL_CIRCLE_SCALE = 0.55f
        private const val HORIZONTAL_CIRCLE_SCALE = 1.25f
        private const val CELESTIAL_BODY_SIZE_MULTIPLIER = 3f
        private const val FAST_FORWARD_DURATION_HOURS = 3f

        private const val HOURS_IN_DAY = 24f
        private const val TWO_PI = (PI * 2).toFloat()
        private const val NINETY_DEGREE_OFFSET_RADIANS = (PI / 2).toFloat()
        private const val MOON_PHASE_SHIFT_RADIANS = PI.toFloat()

        private const val SIZE_SCALING_THRESHOLD_DEGREES = 45.0

        // Keep angle in [0, 2π)
        private fun normalizeRadians(a: Float): Float {
            var x = a % TWO_PI
            if (x < 0f) x += TWO_PI
            return x
        }

        // Hours (0..24) -> angle (0..2π)
        fun hourToAngle(hours: Float): Float {
            val raw = (hours / HOURS_IN_DAY) * TWO_PI - NINETY_DEGREE_OFFSET_RADIANS
            return normalizeRadians(raw)
        }

        // angle (0..2π) -> hours (0..24)
        fun angleToHour(angle: Float): Float {
            val normalized = normalizeRadians(angle + NINETY_DEGREE_OFFSET_RADIANS)
            return (normalized / TWO_PI) * HOURS_IN_DAY
        }
    }
}
