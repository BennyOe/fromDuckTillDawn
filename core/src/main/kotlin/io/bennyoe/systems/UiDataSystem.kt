package io.bennyoe.systems

import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import com.github.quillraven.fleks.World.Companion.inject
import io.bennyoe.components.HealthComponent
import io.bennyoe.components.IsDivingComponent
import io.bennyoe.components.PlayerComponent
import io.bennyoe.ui.GameView

class UiDataSystem(
    private val uiStage: Stage = inject("uiStage"),
) : IteratingSystem(family { all(PlayerComponent, HealthComponent) }) {
    private val gameView by lazy { uiStage.actors.filterIsInstance<GameView>().first() }

    override fun onTickEntity(entity: Entity) {
        val healthCmp = entity[HealthComponent]
        val isDivingCmp = entity.getOrNull(IsDivingComponent)

        if (isDivingCmp != null) {
            val airPercentage = isDivingCmp.currentAir / isDivingCmp.maxAir
            gameView.playerAir(airPercentage)
        } else {
            gameView.playerAir(1f)
        }

        val healthPercentage = healthCmp.current / healthCmp.max

        gameView.playerLife(healthPercentage)
    }
}
