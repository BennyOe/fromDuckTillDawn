package io.bennyoe.systems

import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import com.github.quillraven.fleks.World.Companion.inject
import io.bennyoe.components.CrowComponent
import io.bennyoe.components.DisabledComponent
import io.bennyoe.components.GameStateComponent
import io.bennyoe.components.TimeOfDay
import io.bennyoe.components.TransformComponent
import io.bennyoe.utility.getViewportDimensions
import ktx.log.logger

class CrowSystem(
    private val stage: Stage = inject("stage"),
) : IteratingSystem(family { all(CrowComponent) }),
    PausableSystem {
    private var delay: Float = MathUtils.random(10f, 50f)
    private val gameStateCmp by lazy { world.family { all(GameStateComponent) }.first()[GameStateComponent] }

    override fun onTickEntity(entity: Entity) {
        val transformCmp = entity[TransformComponent]
        val viewportDimensions = getViewportDimensions(stage)

        if (delay > 0) {
            delay -= deltaTime
            return
        }

        if (entity hasNo DisabledComponent &&
            transformCmp.position.x > viewportDimensions.right + transformCmp.width
        ) {
            // remove
            logger.debug { "Crow removed" }
            entity.configure { it += DisabledComponent }
            delay = MathUtils.random(20f, 50f)
            return
        }

        if (gameStateCmp.getTimeOfDay() == TimeOfDay.DAY &&
            entity has DisabledComponent
        ) {
            // spawn new
            logger.debug { "Crow spawned" }
            entity.configure { it -= DisabledComponent }
            val x = viewportDimensions.left - transformCmp.width - 2f
            val yMin = viewportDimensions.bottom + (viewportDimensions.top - viewportDimensions.bottom) * .8f
            val yMax = viewportDimensions.bottom + (viewportDimensions.top - viewportDimensions.bottom) * 0.90f
            val yCenter = MathUtils.random(yMin, yMax)

            transformCmp.position.set(x, yCenter - transformCmp.height * 0.5f)
        }

        // fly
        transformCmp.position.x += 4f * deltaTime
    }

    companion object {
        val logger = logger<CrowSystem>()
    }
}
