package io.bennyoe.systems

import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import com.github.quillraven.fleks.World.Companion.inject
import io.bennyoe.components.AnimationComponent
import io.bennyoe.components.AnimationType
import io.bennyoe.components.AnimationVariant
import io.bennyoe.components.AttackComponent
import io.bennyoe.components.HealthComponent
import io.bennyoe.components.PhysicComponent
import io.bennyoe.components.PlayerComponent
import io.bennyoe.components.debug.DamageTextComponent
import ktx.log.logger
import ktx.math.vec2
import ktx.scene2d.Scene2DSkin

class DamageSystem(
    private val uiStage: Stage = inject("uiStage"),
) : IteratingSystem(family { all(HealthComponent) }) {
    override fun onTickEntity(entity: Entity) {
        val healthCmp = entity[HealthComponent]
        val physicCmp = entity[PhysicComponent]
        val attackCmp = entity[AttackComponent]
        val animationCmp = entity[AnimationComponent]

        if (healthCmp.takenDamage > 0f) {
            logger.debug { "takenDamage: ${healthCmp.takenDamage}" }
            healthCmp.current -= healthCmp.takenDamage
            if (entity hasNo PlayerComponent) {
                animationCmp.nextAnimation(AnimationType.HIT, AnimationVariant.FIRST)
                healthCmp.takenDamage = 0f
            }

            // spawn the damage floating label
            val damageTextCmp = DamageTextComponent(uiStage = uiStage)
            damageTextCmp.txtLocation =
                vec2(
                    physicCmp.body.position.x,
                    physicCmp.body.position.y - physicCmp.size.y * 0.8f,
                )
            damageTextCmp.label = Label("${attackCmp.damage.toInt()} / ${healthCmp.current.toInt()}", Scene2DSkin.defaultSkin)
            entity.configure { it += damageTextCmp }
        }
    }

    companion object {
        val logger = logger<DamageSystem>()
    }
}
