package io.bennyoe.systems

import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import com.github.quillraven.fleks.World.Companion.inject
import io.bennyoe.components.ParticleComponent
import io.bennyoe.components.TransformComponent
import ktx.graphics.use

class ParticleSystem(
    private val stage: Stage = inject("stage"),
) : IteratingSystem(family { all(TransformComponent, ParticleComponent) }) {
    override fun onTickEntity(entity: Entity) {
        val particleCmp = entity[ParticleComponent]
        val transformCmp = entity[TransformComponent]

        if (!particleCmp.active) return

        particleCmp.particleEffect.update(deltaTime)
        particleCmp.particleEffect.setPosition(transformCmp.position.x + particleCmp.offsetX, transformCmp.position.y + particleCmp.offsetY)
        stage.batch.use {
            particleCmp.particleEffect.draw(stage.batch, deltaTime)
        }

        if (!particleCmp.looping) {
            particleCmp.timer += deltaTime
            if (particleCmp.particleEffect.isComplete) {
                particleCmp.active = false
            }
        }
    }
}
