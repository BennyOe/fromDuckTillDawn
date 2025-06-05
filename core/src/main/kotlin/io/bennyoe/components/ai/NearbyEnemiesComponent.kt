package io.bennyoe.components.ai

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import com.github.quillraven.fleks.Entity
import io.bennyoe.components.ai.BehaviorTreeComponent.Companion.NO_TARGET

class NearbyEnemiesComponent(
    val nearbyEntities: MutableSet<Entity> = mutableSetOf(),
) : Component<NearbyEnemiesComponent> {
    var target: Entity = NO_TARGET

    override fun type() = NearbyEnemiesComponent

    companion object : ComponentType<NearbyEnemiesComponent>()
}
