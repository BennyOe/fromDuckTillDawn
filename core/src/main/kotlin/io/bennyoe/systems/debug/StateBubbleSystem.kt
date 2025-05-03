package io.bennyoe.systems.debug

import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.Fixed
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World
import io.bennyoe.GameConstants
import io.bennyoe.components.AiComponent
import io.bennyoe.components.PhysicComponent
import io.bennyoe.components.debug.StateBubbleComponent
import ktx.log.logger
import ktx.math.component1
import ktx.math.component2

class StateBubbleSystem(
    private val stage: Stage = World.Companion.inject("stage"),
) : IteratingSystem(World.Companion.family { all(StateBubbleComponent.Companion) }, interval = Fixed(GameConstants.PHYSIC_TIME_STEP)) {
    override fun onTickEntity(entity: Entity) {
        val stateBubbleCmp = entity[StateBubbleComponent.Companion]
        val aiComponent = entity[AiComponent.Companion]
        stateBubbleCmp.bubble.displayState(aiComponent.stateMachine.currentState.toString())
    }

    override fun onAlphaEntity(
        entity: Entity,
        alpha: Float,
    ) {
        val stateBubbleCmp = entity[StateBubbleComponent.Companion]
        val physicCmp = entity[PhysicComponent.Companion]

        val (prevX, prevY) = physicCmp.prevPos
        val (bodyX, bodyY) = physicCmp.body.position

        val xPos = MathUtils.lerp(prevX, bodyX, alpha)
        val yPos = MathUtils.lerp(prevY + 1.1f, bodyY + 1.1f, alpha)

        val pos = Vector2(xPos, yPos)
        stateBubbleCmp.bubble.setPosition(stage.viewport.project(pos))
    }

    companion object {
        val logger = logger<StateBubbleSystem>()
    }
}
