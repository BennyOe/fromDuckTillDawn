package io.bennyoe.components
import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

class AnimationCollectionComponent(
    val animations: MutableList<AnimationType> = mutableListOf()
) : Component<AnimationCollectionComponent> {
    override fun type() = AnimationCollectionComponent

    companion object : ComponentType<AnimationCollectionComponent>()
}
