package io.bennyoe.systems

import com.badlogic.gdx.ai.msg.MessageManager
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import com.github.quillraven.fleks.World.Companion.inject
import io.bennyoe.components.AnimationComponent
import io.bennyoe.components.AnimationType
import io.bennyoe.components.AttackComponent
import io.bennyoe.components.HealthComponent
import io.bennyoe.components.PhysicComponent
import io.bennyoe.components.PlayerComponent
import io.bennyoe.components.StateComponent
import io.bennyoe.components.debug.DamageTextComponent
import io.bennyoe.state.FsmMessageTypes
import ktx.log.logger
import ktx.math.vec2
import ktx.scene2d.Scene2DSkin

class DamageSystem(
    private val uiStage: Stage = inject("uiStage"),
) : IteratingSystem(family { all(HealthComponent) }),
    PausableSystem {
    private val messageDispatcher = MessageManager.getInstance()

    override fun onTickEntity(entity: Entity) {
        val healthCmp = entity[HealthComponent]
        val physicCmp = entity[PhysicComponent]
        val attackCmp = entity[AttackComponent]
        val animationCmp = entity[AnimationComponent]
        val stateCmp = entity[StateComponent]
        val baseDamageString = attackCmp.attackMap[attackCmp.appliedAttack]?.baseDamage ?: 0f

        if (healthCmp.takenDamage > 0f) {
            logger.debug { "takenDamage: ${healthCmp.takenDamage}" }
            healthCmp.current -= healthCmp.takenDamage
            healthCmp.takenDamage = 0f

            if (entity hasNo PlayerComponent) {
                animationCmp.nextAnimation(AnimationType.HIT)
            }

            if (entity has PlayerComponent) {
                messageDispatcher.dispatchMessage(FsmMessageTypes.PLAYER_IS_HIT.ordinal)
            } else {
                messageDispatcher.dispatchMessage(0f, stateCmp.stateMachine, stateCmp.stateMachine, FsmMessageTypes.ENEMY_IS_HIT.ordinal)
                // spawn the damage floating label
                val damageTextCmp = DamageTextComponent(uiStage = uiStage)
                damageTextCmp.txtLocation =
                    vec2(
                        physicCmp.body.position.x,
                        physicCmp.body.position.y - physicCmp.size.y * 0.8f,
                    )
                damageTextCmp.label = Label("${baseDamageString.toInt()} / ${healthCmp.current.toInt()}", Scene2DSkin.defaultSkin)
                entity.configure { it += damageTextCmp }
            }
        }
    }

    companion object {
        val logger = logger<DamageSystem>()
    }
}
