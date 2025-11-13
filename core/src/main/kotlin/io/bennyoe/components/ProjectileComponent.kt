package io.bennyoe.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import io.bennyoe.config.EntityCategory

class ProjectileComponent(
    val entityCategory: EntityCategory,
) : Component<ProjectileComponent> {
    var isThrown: Boolean = false

    override fun type() = ProjectileComponent

    companion object : ComponentType<ProjectileComponent>()
}
