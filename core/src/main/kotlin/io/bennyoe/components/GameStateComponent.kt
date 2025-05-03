package io.bennyoe.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

class GameStateComponent : Component<GameStateComponent> {
    override fun type() = GameStateComponent

    companion object : ComponentType<GameStateComponent>()
}
