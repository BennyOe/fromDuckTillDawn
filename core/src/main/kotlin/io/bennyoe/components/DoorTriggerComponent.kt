package io.bennyoe.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import com.github.quillraven.fleks.Entity

class DoorTriggerComponent(
    val targetEntity: Entity,
) : Component<DoorTriggerComponent> {
    override fun type() = DoorTriggerComponent

    companion object : ComponentType<DoorTriggerComponent>()
}
