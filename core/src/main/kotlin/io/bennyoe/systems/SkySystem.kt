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
import io.bennyoe.event.MapChangedEvent
import io.bennyoe.utility.hourToAngle
import io.bennyoe.utility.isHourIn
import io.bennyoe.utility.nightFactor
import ktx.tiled.height
import ktx.tiled.width
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

class SkySystem :
    IteratingSystem(
        family {
            all(SkyComponent, TransformComponent)
            any(ImageComponent, ParticleComponent)
        },
    ),
    EventListener,
    PausableSystem {
    private val gameStateCmp by lazy { world.family { all(GameStateComponent) }.first()[GameStateComponent] }

    private var mapWidth = 0f
    private var mapHeight = 0f
    private var centerX = 0f
    private var centerY = 0f
    private var horizontalRadius = 0f
    private var verticalRadius = 0f

    companion object {
        private const val VERTICAL_CIRCLE_SCALE = 1f
        private const val HORIZONTAL_CIRCLE_SCALE = 1f
        private const val CELESTIAL_BODY_SIZE_MULTIPLIER = 3f
        private const val MOON_PHASE_SHIFT_RADIANS = PI.toFloat()
        private const val SIZE_SCALING_THRESHOLD_DEGREES = 45.0
    }

    override fun handle(event: Event): Boolean {
        if (event is MapChangedEvent) {
            mapWidth = event.map.width.toFloat()
            mapHeight = event.map.height.toFloat()
            centerX = mapWidth / 2f
            centerY = mapHeight / 2f
            horizontalRadius = mapWidth / 2f
            verticalRadius = mapHeight / 2f
            return true
        }
        return false
    }

    override fun onTickEntity(entity: Entity) {
        val currentHour = gameStateCmp.timeOfDay
        val currentAngle = hourToAngle(currentHour)

        val skyCmp = entity[SkyComponent]
        val transformCmp = entity[TransformComponent]
        val lightCmp = entity.getOrNull(LightComponent)
        val imageCmp = entity.getOrNull(ImageComponent)
        val particleCmp = entity.getOrNull(ParticleComponent)

        updateSkyVisuals(skyCmp, imageCmp, particleCmp, currentHour)
        updateLightStatus(skyCmp, lightCmp, currentHour)

        val bodyAngle =
            when (skyCmp.type) {
                SkyComponentType.SUN -> currentAngle
                SkyComponentType.MOON -> currentAngle + MOON_PHASE_SHIFT_RADIANS
                else -> return // Other sky components don't orbit
            }

        updateBodyTransform(transformCmp, bodyAngle)
    }

    private fun updateSkyVisuals(
        skyCmp: SkyComponent,
        imageCmp: ImageComponent?,
        particleCmp: ParticleComponent?,
        hour: Float,
    ) {
        val nFactor = nightFactor(hour)
        when (skyCmp.type) {
            SkyComponentType.SKY -> {
                imageCmp?.image?.color?.let {
                    it.set(Color.WHITE)
                    it.lerp(Color.DARK_GRAY, nFactor)
                    it.a = 1f
                }
            }

            SkyComponentType.STARS -> {
                imageCmp?.image?.color?.a = nFactor
            }

            SkyComponentType.SHOOTING_STAR -> {
                particleCmp?.enabled = (gameStateCmp.getTimeOfDay() == TimeOfDay.NIGHT)
            }

            else -> Unit
        }
    }

    private fun updateLightStatus(
        skyCmp: SkyComponent,
        lightCmp: LightComponent?,
        hour: Float,
    ) {
        val on =
            when (skyCmp.type) {
                SkyComponentType.SUN -> isHourIn(hour, 4f, 19f)
                SkyComponentType.MOON -> isHourIn(hour, 17f, 6f)
                else -> false
            }
        lightCmp?.gameLight?.setOn(on)
    }

    private fun updateBodyTransform(
        transformCmp: TransformComponent,
        bodyAngle: Float,
    ) {
        val x = centerX + cos(bodyAngle) * horizontalRadius * HORIZONTAL_CIRCLE_SCALE
        val y = centerY + sin(bodyAngle) * verticalRadius * VERTICAL_CIRCLE_SCALE

        val size = getDynamicSize(y) * CELESTIAL_BODY_SIZE_MULTIPLIER
        transformCmp.position.set(x, y)
        transformCmp.width = size
        transformCmp.height = size
    }

    private fun getDynamicSize(y: Float): Float {
        val distanceToCenter = abs(y - centerY)
        val threshold = verticalRadius * sin(Math.toRadians(SIZE_SCALING_THRESHOLD_DEGREES)).toFloat()
        val normalized = ((threshold - distanceToCenter) / threshold).coerceIn(0f, 1f)
        return 1f + normalized * normalized * 2f
    }
}
