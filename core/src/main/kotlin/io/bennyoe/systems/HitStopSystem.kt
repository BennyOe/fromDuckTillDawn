package io.bennyoe.systems

import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import io.bennyoe.components.HitEffectComponent

class HitStopSystem :
    IteratingSystem(family { all(HitEffectComponent) }),
    EventListener {
    override fun handle(event: Event): Boolean = false

    override fun onTickEntity(entity: Entity) {
        val hitEffectCmp = entity[HitEffectComponent]
        hitEffectCmp.timer += deltaTime

        if (hitEffectCmp.isFinished) {
            // Remove the component once the effect's duration is over.
            entity.configure { it -= HitEffectComponent }
        }
    }
}
