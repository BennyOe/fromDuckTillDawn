package io.bennyoe.systems

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.physics.box2d.World
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import com.github.quillraven.fleks.World.Companion.inject
import io.bennyoe.components.AttackComponent
import io.bennyoe.components.AttackType
import io.bennyoe.components.HealthComponent
import io.bennyoe.components.ImageComponent
import io.bennyoe.components.PhysicComponent
import io.bennyoe.components.TimeScaleComponent
import io.bennyoe.systems.debug.DebugRenderer
import io.bennyoe.systems.debug.DebugType
import io.bennyoe.systems.debug.addToDebugView
import io.bennyoe.utility.EntityBodyData
import io.bennyoe.utility.SensorType
import io.bennyoe.utility.fixtureData
import ktx.box2d.query
import ktx.log.logger
import ktx.math.component1
import ktx.math.component2

class AttackSystem(
    private val debugRenderer: DebugRenderer = inject("debugRenderService"),
    private val phyWorld: World = inject("phyWorld"),
) : IteratingSystem(family { all(AttackComponent) }),
    PausableSystem {
    private var attackDelayCounter = 0f
    private val timeScaleCmp by lazy { world.family { all(TimeScaleComponent) }.first()[TimeScaleComponent] }

    override fun onTickEntity(entity: Entity) {
        val attackCmp = entity[AttackComponent]
        val physicCmp = entity[PhysicComponent]
        val imageCmp = entity[ImageComponent]

        if (attackCmp.appliedAttack == AttackType.NONE) return

        val appliedAttack = attackCmp.attackMap[attackCmp.appliedAttack]!!

        if (attackDelayCounter < appliedAttack.attackDelay) {
            attackDelayCounter += deltaTime
            return
        }

        // set the hit stop if it is available
        timeScaleCmp.isHitStopActive = appliedAttack.hasHitStop
        timeScaleCmp.currentHitStopDuration = appliedAttack.hitStopDuration

        attackCmp.appliedAttack = AttackType.NONE
        attackDelayCounter = 0f

        val leftAttack = imageCmp.flipImage
        val (x, y) = physicCmp.body.position
        val (w, h) = physicCmp.size
        val halfW = w * 0.5f
        val halfH = h * 0.5f

        val attackX = if (leftAttack) x - halfW - appliedAttack.extraRange else x + halfW
        AABB_Rect
            .set(
                attackX,
                y - halfH,
                appliedAttack.extraRange,
                h,
            ).addToDebugView(debugRenderer, Color.GOLD, "attack box", ShapeRenderer.ShapeType.Filled, 0.4f, 1f, DebugType.ATTACK)

        // queries the AABB rect if there are any fixtures inside
        phyWorld.query(
            AABB_Rect.x,
            AABB_Rect.y,
            AABB_Rect.x + AABB_Rect.width,
            AABB_Rect.y + AABB_Rect.height,
        ) { fixture ->
            if (fixture.fixtureData?.sensorType != SensorType.HITBOX_SENSOR) {
                return@query true
            }

            // not hitting me
            val bodyData = fixture.body.userData as EntityBodyData
            if (bodyData.entity == entity) {
                return@query true
            }

            // check to not hit entities in same category
            if (physicCmp.categoryBits == fixture.filterData.categoryBits) {
                return@query true
            }

            // not hitting dead enemies
            if (bodyData.entity[HealthComponent].isDead) return@query true

            logger.debug { "Fixture found" }
            bodyData.entity.configure {
                val healthCmp = it.getOrNull(HealthComponent)
                healthCmp?.takeDamage(appliedAttack.baseDamage)

                // Retrieve the victim's image component to check facing direction
                val victimImageCmp = it.getOrNull(ImageComponent)

                if (victimImageCmp != null) {
                    val victimX = fixture.body.position.x

                    val isAttackerRight = x > victimX
                    val isVictimFacingRight = !victimImageCmp.flipImage

                    // Logic: Victim faces Right & Attacker is Left OR Victim faces Left & Attacker is Right
                    healthCmp?.attackedFromBehind = (isVictimFacingRight && !isAttackerRight) || (!isVictimFacingRight && isAttackerRight)
                } else {
                    // Fallback if no ImageComponent exists (e.g. non-visual entity)
                    healthCmp?.attackedFromBehind = false
                }
            }
            return@query true
        }
    }

    companion object {
        val AABB_Rect = Rectangle()
        val logger = logger<AttackSystem>()
    }
}
