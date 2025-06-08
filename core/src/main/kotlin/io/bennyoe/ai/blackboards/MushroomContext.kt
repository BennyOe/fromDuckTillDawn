package io.bennyoe.ai.blackboards

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import io.bennyoe.components.AnimationComponent
import io.bennyoe.components.HealthComponent
import io.bennyoe.components.IntentionComponent
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
    val debugRenderService: DebugRenderService,
) : AbstractBlackboard(entity, world, stage) {
    val nearbyEnemiesCmp: NearbyEnemiesComponent
    val phyCmp: PhysicComponent
    val animCmp: AnimationComponent
    val intentionCmp: IntentionComponent
    val healthCmp: HealthComponent
    val location: Vector2
        get() = phyCmp.body.position

    init {
        with(world) {
            nearbyEnemiesCmp = entity[NearbyEnemiesComponent]
            phyCmp = entity[PhysicComponent]
            animCmp = entity[AnimationComponent]
            healthCmp = entity[HealthComponent]
            intentionCmp = entity[IntentionComponent]
        }
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

    fun isAlive(): Boolean = !healthCmp.isDead

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
            ).addToDebugView(debugRenderService, Color.BLACK, "range")
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
        intentionCmp.wantsToAttack = true
    }

    fun stopAttack() {
        intentionCmp.wantsToAttack = false
    }

    companion object {
        val logger = logger<MushroomContext>()
        val TMP_RECT = Rectangle()
    }
}
