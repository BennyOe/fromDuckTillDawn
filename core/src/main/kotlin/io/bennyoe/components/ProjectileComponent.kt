package io.bennyoe.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import io.bennyoe.config.EntityCategory

class ProjectileComponent(
    val damage: Float,
    val type: ProjectileType,
) : Component<ProjectileComponent> {
    var isThrown: Boolean = false
    var hitGround: Boolean = false

    override fun type() = ProjectileComponent

    companion object : ComponentType<ProjectileComponent>()
}

enum class ProjectileType {
    ROCK,
}
