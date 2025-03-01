package io.bennyoe.systems

import com.badlogic.gdx.physics.box2d.World
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.Fixed
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import com.github.quillraven.fleks.World.Companion.inject
import io.bennyoe.components.ImageComponent
import io.bennyoe.components.PhysicComponent
import ktx.log.logger
import ktx.math.component1
import ktx.math.component2

class PhysicsSystem(
    private val phyWorld: World = inject("phyWorld"),
) : IteratingSystem(family { all(PhysicComponent, ImageComponent) }, interval = Fixed(1 / 60f)) {

    override fun onUpdate() {
        if (phyWorld.autoClearForces) {
            LOG.error { "AutoClearForces must be set to false" }
            phyWorld.autoClearForces = false
        }
        super.onUpdate()
        phyWorld.clearForces()
    }

    override fun onTick() {
        super.onTick()
        phyWorld.step(deltaTime, 6, 2)
    }

    override fun onTickEntity(entity: Entity) {
        val imageCmp = entity[ImageComponent]
        val physicCmp = entity[PhysicComponent]

        val (bodyX, bodyY) = physicCmp.body.position
        imageCmp.image.run {
            setPosition(bodyX - width * 0.5f, bodyY - height * 0.5f)
        }
    }

    companion object {
        private val LOG = logger<PhysicsSystem>()
    }
}
