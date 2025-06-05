package io.bennyoe.components.ai

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ai.btree.BehaviorTree
import com.badlogic.gdx.ai.btree.utils.BehaviorTreeParser
import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import io.bennyoe.ai.blackboards.AbstractBlackboard

// class has to be generic because of the individual blackboard for every entity
class BehaviorTreeComponent<T : AbstractBlackboard>(
    val world: World,
    val stage: Stage,
    val treePath: String = "",
    val createBlackboard: (Entity, World, Stage) -> T,
) : Component<BehaviorTreeComponent<T>> {
    @Suppress("UNCHECKED_CAST")
    override fun type(): ComponentType<BehaviorTreeComponent<T>> = Companion as ComponentType<BehaviorTreeComponent<T>>

    lateinit var behaviorTree: BehaviorTree<T>
    lateinit var blackboard: T
    private val bTreeParser = BehaviorTreeParser<T>()

    override fun World.onAdd(entity: Entity) {
        if (treePath.isNotBlank()) {
            blackboard = createBlackboard(entity, world, stage)
            behaviorTree =
                bTreeParser.parse(
                    Gdx.files.internal(treePath),
                    blackboard,
                )
        }
    }

    companion object : ComponentType<BehaviorTreeComponent<*>>() {
        val NO_TARGET = Entity.Companion.NONE
    }
}
