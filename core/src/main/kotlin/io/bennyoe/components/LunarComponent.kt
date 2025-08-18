package io.bennyoe.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

class LunarComponent : Component<LunarComponent> {
    override fun type() = LunarComponent

    companion object : ComponentType<LunarComponent>()
}
