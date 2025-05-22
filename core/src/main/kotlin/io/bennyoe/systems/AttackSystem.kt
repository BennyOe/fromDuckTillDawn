package io.bennyoe.systems

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.physics.box2d.World
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import com.github.quillraven.fleks.World.Companion.inject
import io.bennyoe.components.AttackComponent
import io.bennyoe.components.HealthComponent
import io.bennyoe.components.ImageComponent
import io.bennyoe.components.PhysicComponent
import io.bennyoe.service.DebugRenderService
import io.bennyoe.service.addToDebugView
import ktx.box2d.query
import ktx.log.logger
import ktx.math.component1
import ktx.math.component2

class AttackSystem(
    private val debugRenderService: DebugRenderService = inject("debugRenderService"),
    private val phyWorld: World = inject("phyWorld"),
) : IteratingSystem(family { all(AttackComponent) }) {
    override fun onTickEntity(entity: Entity) {
        val attackCmp = entity[AttackComponent]
        val physicCmp = entity[PhysicComponent]
        val imageCmp = entity[ImageComponent]

        if (!attackCmp.applyAttack) return
        attackCmp.applyAttack = false

        val leftAttack = imageCmp.flipImage
        val (x, y) = physicCmp.body.position
        val (w, h) = physicCmp.size
        val halfW = w * 0.5f
        val halfH = h * 0.5f

        val attackX = if (leftAttack) x - halfW - attackCmp.extraRange else x + halfW
        AABB_Rect
            .set(
                attackX,
                y - halfH,
                attackCmp.extraRange,
                h,
            ).addToDebugView(debugRenderService, Color.GOLD, "attack box", ShapeRenderer.ShapeType.Filled, 0.4f, 1f)

        // queries the AABB rect if there are any fixtures inside
        phyWorld.query(
            AABB_Rect.x,
            AABB_Rect.y,
            AABB_Rect.x + AABB_Rect.width,
            AABB_Rect.y + AABB_Rect.height,
        ) { fixture ->
            if (fixture.userData != "HITBOX_SENSOR") {
                return@query true
            }

            val fixtureEntity = fixture.body.userData as Entity
            if (fixtureEntity == entity) {
                return@query true
            }

            // TODO implement checks for only hitting enemies

            logger.debug { "Fixture found" }
            fixtureEntity.configure {
                val healthCmp = it.getOrNull(HealthComponent)
                healthCmp?.takenDamage += attackCmp.damage * MathUtils.random(0.9f, 1.1f)
            }
            return@query true
        }
    }

    companion object {
        val AABB_Rect = Rectangle()
        val logger = logger<AttackSystem>()
    }
}
