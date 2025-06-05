package io.bennyoe.ai.blackboards

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import io.bennyoe.components.AnimationComponent
import io.bennyoe.components.AnimationType
import io.bennyoe.components.AnimationVariant
import io.bennyoe.components.AttackComponent
import io.bennyoe.components.HealthComponent
import io.bennyoe.components.ImageComponent
import io.bennyoe.components.IntentionComponent
import io.bennyoe.components.MoveComponent
import io.bennyoe.components.PhysicComponent
import io.bennyoe.components.PlayerComponent
import io.bennyoe.components.WalkDirection
import io.bennyoe.components.ai.BehaviorTreeComponent
import io.bennyoe.components.ai.NearbyEnemiesComponent
import io.bennyoe.service.DebugRenderService
import io.bennyoe.service.addToDebugView
import ktx.log.logger
import ktx.math.compareTo
import ktx.math.component1
import ktx.math.component2

class MushroomContext(
    entity: Entity,
    world: World,
    stage: Stage,
) : AbstractBlackboard(entity, world, stage) {
    val nearbyEnemiesCmp: NearbyEnemiesComponent
    val phyCmp: PhysicComponent
    val animCmp: AnimationComponent
    val imageCmp: ImageComponent
    val intentionCmp: IntentionComponent
    val moveCmp: MoveComponent
    val healthComponent: HealthComponent
    val attackCmp: AttackComponent
    val location: Vector2
        get() = phyCmp.body.position

    init {
        with(world) {
            nearbyEnemiesCmp = entity[NearbyEnemiesComponent]
            phyCmp = entity[PhysicComponent]
            animCmp = entity[AnimationComponent]
            imageCmp = entity[ImageComponent]
            moveCmp = entity[MoveComponent]
            healthComponent = entity[HealthComponent]
            attackCmp = entity[AttackComponent]
            intentionCmp = entity[IntentionComponent]
        }
    }

    fun setAnimation(
        type: AnimationType,
        playMode: Animation.PlayMode = Animation.PlayMode.LOOP,
        variant: AnimationVariant = AnimationVariant.FIRST,
        resetStateTime: Boolean = false,
        isReversed: Boolean = false,
    ) {
        animCmp.nextAnimation(type, variant)
        if (resetStateTime) animCmp.stateTime = 0f
        animCmp.isReversed = isReversed
        animCmp.mode = playMode
    }

    fun moveTo(targetPos: Vector2) {
        logger.debug { "Location: ${location.x} Target: ${targetPos.x}" }
        if (location < targetPos) {
            intentionCmp.walkDirection = WalkDirection.RIGHT
        }
        if (location > targetPos) {
            intentionCmp.walkDirection = WalkDirection.LEFT
        }
    }

    fun isAlive(): Boolean = !healthComponent.isDead

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
            ).addToDebugView(DebugRenderService, Color.BLACK, "range")
        return TMP_RECT.contains(targetPos)
    }

    fun stopMovement() {
        intentionCmp.walkDirection = WalkDirection.NONE
    }

    // TODO implement
    fun canAttack(): Boolean = true

    fun hasEnemyNearby(): Boolean {
        with(world) {
            nearbyEnemiesCmp.target = nearbyEnemiesCmp.nearbyEntities
                .firstOrNull {
                    it has PlayerComponent.Companion
                } ?: BehaviorTreeComponent.Companion.NO_TARGET
        }
        return nearbyEnemiesCmp.target != BehaviorTreeComponent.Companion.NO_TARGET
    }

    fun isAnimationFinished(): Boolean = animCmp.isAnimationFinished()

    // TODO implement
    fun startAttack() {
        attackCmp.applyAttack = true
    }

    companion object {
        val logger = logger<MushroomContext>()
        val TMP_RECT = Rectangle()
    }
}
