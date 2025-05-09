package io.bennyoe.systems.debug

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.Fixed
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import com.github.quillraven.fleks.World.Companion.inject
import io.bennyoe.GameConstants.PHYSIC_TIME_STEP
import io.bennyoe.components.AiComponent
import io.bennyoe.components.PhysicComponent
import io.bennyoe.components.debug.StateBubbleComponent
import ktx.log.logger
import ktx.math.component1
import ktx.math.component2

private const val STATE_BUBBLE_OFFSET_Y = 1.1f

class StateBubbleSystem(
    private val stage: Stage = inject("stage"),
    private val uiStage: Stage = inject("uiStage"),
) : IteratingSystem(family { all(StateBubbleComponent) }, interval = Fixed(PHYSIC_TIME_STEP)) {
    override fun onTickEntity(entity: Entity) {
        val stateBubbleCmp = entity[StateBubbleComponent]
        val aiComponent = entity[AiComponent]
        stateBubbleCmp.bubble.displayState(aiComponent.stateMachine.currentState.toString())
    }

    override fun onAlphaEntity(
        entity: Entity,
        alpha: Float,
    ) {
        val stateBubbleCmp = entity[StateBubbleComponent]
        val physicCmp = entity[PhysicComponent]

        // interpolate WorldUnit positions
        val (prevX, prevY) = physicCmp.prevPos
        val (bodyX, bodyY) = physicCmp.body.position
        val xPosWU = MathUtils.lerp(prevX, bodyX, alpha)
        val yPosWU = MathUtils.lerp(prevY + STATE_BUBBLE_OFFSET_Y, bodyY + STATE_BUBBLE_OFFSET_Y, alpha)

        // world units -> pixel transformation (with camera not viewport because the uiStage calculates the viewport already)
        val screenVec = stage.camera.project(Vector3(xPosWU, yPosWU, 0f))

        // LibGDX screen coordinates have their origin at the BOTTOM-left,
        // Scene2D.screenToStageCoordinates expects the origin at the TOP-left.
        val screenX = screenVec.x
        val screenY = Gdx.graphics.height - screenVec.y

        // pixel -> uiStage WU
        val uiCoords = Vector2(screenX, screenY)
        uiStage.screenToStageCoordinates(uiCoords)

        // place bubbles
        stateBubbleCmp.bubble.setPosition(
            uiCoords.x - stateBubbleCmp.bubble.displayState.width * 0.5f,
            uiCoords.y,
        )
    }

    companion object {
        val logger = logger<StateBubbleSystem>()
    }
}
