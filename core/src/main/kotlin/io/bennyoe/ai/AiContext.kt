package io.bennyoe.ai

import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import io.bennyoe.components.AiComponent
import io.bennyoe.components.AnimationComponent
import io.bennyoe.components.AnimationModel
import io.bennyoe.components.AnimationType
import io.bennyoe.components.AnimationVariant
import io.bennyoe.components.MoveComponent
import io.bennyoe.components.PhysicComponent
import io.bennyoe.service.DebugRenderService
import io.bennyoe.service.addToDebugView
import ktx.math.compareTo
import ktx.math.component1
import ktx.math.component2

class AiContext(
    val entity: Entity,
    world: World,
    private val stage: Stage,
) {
    val aiCmp: AiComponent
    val phyCmp: PhysicComponent
    val animCmp: AnimationComponent
    val moveCmp: MoveComponent
    var currentTask: Action = IdleTask()
    val location: Vector2
        get() = phyCmp.body.position

    init {
        with(world) {
            aiCmp = entity[AiComponent]
            phyCmp = entity[PhysicComponent]
            animCmp = entity[AnimationComponent]
            moveCmp = entity[MoveComponent]
        }
    }

    fun setAnimation(
        type: AnimationType,
        playMode: Animation.PlayMode = Animation.PlayMode.LOOP,
        variant: AnimationVariant = AnimationVariant.FIRST,
        resetStateTime: Boolean = false,
        isReversed: Boolean = false,
    ) {
        animCmp.nextAnimation(AnimationModel.ENEMY_MUSHROOM, type, variant)
        if (resetStateTime) animCmp.stateTime = 0f
        animCmp.isReversed = isReversed
        animCmp.mode = playMode
    }

    fun moveTo(
        startPos: Vector2,
        targetPos: Vector2,
    ) {
        if (startPos < targetPos) moveCmp.moveVelocity = 2f
        if (startPos > targetPos) moveCmp.moveVelocity = -2f
    }

    fun inRange(
        range: Float,
        targetPos: Vector2,
    ): Boolean {
        val (sourceX, sourceY) = phyCmp.body.position
        val (sourceOffX, sourceOffY) = phyCmp.offset
        var (sourceSizeX, sourceSizeY) = phyCmp.size
        sourceSizeX += range
        sourceSizeY += range

        TMP_RECT
            .set(
                sourceOffX + sourceX - sourceSizeX * 0.5f,
                sourceOffY + sourceY - sourceSizeY * 0.5f,
                sourceSizeX,
                sourceSizeY,
            ).addToDebugView(DebugRenderService)
        return TMP_RECT.contains(targetPos)
    }

    fun stopMovement() {
        moveCmp.moveVelocity = 0f
    }

    companion object {
        val TMP_RECT = Rectangle()
    }
}
