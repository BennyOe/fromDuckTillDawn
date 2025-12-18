package io.bennyoe.systems

import com.badlogic.gdx.ai.msg.MessageManager
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import com.github.quillraven.fleks.World.Companion.inject
import io.bennyoe.components.AttackComponent
import io.bennyoe.components.CharacterTypeComponent
import io.bennyoe.components.HealthComponent
import io.bennyoe.components.PhysicComponent
import io.bennyoe.components.StateComponent
import io.bennyoe.components.debug.DamageTextComponent
import io.bennyoe.config.CharacterType
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
        val stateCmp = entity[StateComponent]
        val characterTypeCmp = entity[CharacterTypeComponent]
        val baseDamageString = attackCmp.attackMap[attackCmp.appliedAttack]?.baseDamage ?: 0f

        if (healthCmp.takenDamage > 0f) {
            when (characterTypeCmp.characterType) {
                CharacterType.PLAYER -> {
                    logger.debug { "takenDamage: ${healthCmp.takenDamage}" }
                    healthCmp.current -= healthCmp.takenDamage
                    healthCmp.takenDamage = 0f
                    messageDispatcher.dispatchMessage(FsmMessageTypes.PLAYER_IS_HIT.ordinal)
                }

                CharacterType.MINOTAUR -> {
                    if (!healthCmp.attackedFromBehind) {
                        spawnDamageLabel(physicCmp, baseDamageString, healthCmp, entity, true)
                        return
                    }
                    healthCmp.current -= healthCmp.takenDamage
                    healthCmp.takenDamage = 0f
                    messageDispatcher.dispatchMessage(
                        0f,
                        stateCmp.stateMachine,
                        stateCmp.stateMachine,
                        FsmMessageTypes.ENEMY_IS_HIT.ordinal,
                    )
                    spawnDamageLabel(physicCmp, baseDamageString, healthCmp, entity)
                }

                else -> {
                    healthCmp.current -= healthCmp.takenDamage
                    healthCmp.takenDamage = 0f
                    messageDispatcher.dispatchMessage(
                        0f,
                        stateCmp.stateMachine,
                        stateCmp.stateMachine,
                        FsmMessageTypes.ENEMY_IS_HIT.ordinal,
                    )
                    spawnDamageLabel(physicCmp, baseDamageString, healthCmp, entity)
                }
            }
        }
    }

    private fun spawnDamageLabel(
        physicCmp: PhysicComponent,
        baseDamageString: Float,
        healthCmp: HealthComponent,
        entity: Entity,
        blocked: Boolean = false,
    ) {
        val damageTextCmp = DamageTextComponent(uiStage = uiStage)
        damageTextCmp.txtLocation =
            vec2(
                physicCmp.body.position.x,
                physicCmp.body.position.y - physicCmp.size.y * 0.8f,
            )
        if (blocked) {
            damageTextCmp.label = Label("Blocked!", Scene2DSkin.defaultSkin)
        } else {
            damageTextCmp.label = Label("${baseDamageString.toInt()} / ${healthCmp.current.toInt()}", Scene2DSkin.defaultSkin)
        }
        entity.configure { it += damageTextCmp }
    }

    companion object {
        val logger = logger<DamageSystem>()
    }
}
