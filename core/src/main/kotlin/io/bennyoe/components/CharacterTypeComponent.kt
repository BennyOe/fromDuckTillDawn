package io.bennyoe.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import io.bennyoe.config.CharacterType

class CharacterTypeComponent(
    val characterType: CharacterType,
) : Component<CharacterTypeComponent> {
    override fun type() = CharacterTypeComponent

    companion object : ComponentType<CharacterTypeComponent>()
}
