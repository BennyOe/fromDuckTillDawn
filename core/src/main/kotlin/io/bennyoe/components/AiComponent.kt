package io.bennyoe.components

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ai.btree.BehaviorTree
import com.badlogic.gdx.ai.btree.utils.BehaviorTreeParser
import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import io.bennyoe.ai.AiContext

class AiComponent(
    val world: World,
    val stage: Stage,
    val nearbyEntities: MutableSet<Entity> = mutableSetOf(),
    val treePath: String = "",
) : Component<AiComponent> {
    override fun type() = AiComponent

    lateinit var behaviorTree: BehaviorTree<AiContext>
    private val bTreeParser = BehaviorTreeParser<AiContext>()

    override fun World.onAdd(entity: Entity) {
        if (treePath.isNotBlank()) {
            behaviorTree =
                bTreeParser.parse(
                    Gdx.files.internal(treePath),
                    AiContext(entity, world, stage),
                )
        }
    }

    companion object : ComponentType<AiComponent>()
}
