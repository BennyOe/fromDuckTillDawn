package io.bennyoe.components

import com.badlogic.gdx.math.Vector2
import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import ktx.math.vec2

data class SpawnComponent(
    var type: String = "",
    var location: Vector2 = vec2(),
) : Component<SpawnComponent> {
    override fun type() = SpawnComponent

    companion object : ComponentType<SpawnComponent>()
}

data class SpawnCfg(
    val model: AnimationModel,
    val type: AnimationType,
    val variant: AnimationVariant,
)
