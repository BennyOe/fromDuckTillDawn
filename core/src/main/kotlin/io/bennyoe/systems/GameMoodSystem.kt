package io.bennyoe.systems

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import io.bennyoe.components.GameMood
import io.bennyoe.components.GameStateComponent
import io.bennyoe.components.HealthComponent
import io.bennyoe.components.PlayerComponent
import io.bennyoe.components.ai.BehaviorTreeComponent

class GameMoodSystem : IteratingSystem(family { all(BehaviorTreeComponent) }) {
    private val gameStateEntity by lazy { world.family { all(GameStateComponent) }.first() }
    private val playerEntity by lazy { world.family { all(PlayerComponent) }.first() }
    private var highestMoodThisTick = GameMood.NORMAL

    override fun onTick() {
        highestMoodThisTick = GameMood.NORMAL

        super.onTick()

        val gameStateCmp = with(world) { gameStateEntity[GameStateComponent] }
        if (gameStateCmp.gameMood != highestMoodThisTick) {
            gameStateCmp.gameMood = highestMoodThisTick
        }
    }

    override fun onTickEntity(entity: Entity) {
        val behaviorTreeCmp = entity[BehaviorTreeComponent]
        val currentEntityMood = behaviorTreeCmp.behaviorTree.`object`.currentMood
        val playerHealthCmp = playerEntity[HealthComponent]

        if (playerHealthCmp.isDead) {
            highestMoodThisTick = GameMood.PLAYER_DEAD
        }

        if (currentEntityMood.priority > highestMoodThisTick.priority) {
            highestMoodThisTick = currentEntityMood
        }
    }
}
