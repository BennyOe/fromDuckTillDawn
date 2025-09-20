package io.bennyoe.systems.audio

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import io.bennyoe.components.IsDiving
import io.bennyoe.components.PlayerComponent
import io.bennyoe.components.StateComponent

class UnderWaterFilterSystem : IteratingSystem(family { all(PlayerComponent, StateComponent) }) {
    private val reverb = world.system<ReverbSystem>()

    override fun onTickEntity(entity: Entity) {
        if (entity hasNo IsDiving) {
            reverb.setGlobalFilters(1f, 1f)
        } else {
            reverb.setGlobalFilters(1f, 0.002f)
        }
    }
}
