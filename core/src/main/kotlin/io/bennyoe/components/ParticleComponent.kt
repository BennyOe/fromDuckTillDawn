package io.bennyoe.components

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.g2d.ParticleEffect
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import io.bennyoe.actors.ParticleActor

class ParticleComponent(
    private val stage: Stage,
    val type: ParticleType,
    var zIndex: Int = 0,
    val particleFile: FileHandle,
    val scaleFactor: Float = 1f,
    val motionScaleFactor: Float = 1f,
    var looping: Boolean = true,
    var offsetX: Float = 0f,
    var offsetY: Float = 0f,
    var enabled: Boolean = true,
) : Component<ParticleComponent> {
    lateinit var actor: ParticleActor

    override fun type() = ParticleComponent

    override fun World.onAdd(entity: Entity) {
        val particlesAtlas: TextureAtlas = inject("particlesAtlas")
        val particleEffect = ParticleEffect()
        particleEffect.load(particleFile, particlesAtlas)
        particleEffect.scaleEffect(scaleFactor, motionScaleFactor)

        if (!looping) {
            particleEffect.emitters.forEach { it.isContinuous = false }
        }

        particleEffect.start()

        actor = ParticleActor(particleEffect)
        actor.userObject = entity
        stage.addActor(actor)
    }

    override fun World.onRemove(entity: Entity) {
        actor.remove()
        actor.dispose()
    }

    companion object : ComponentType<ParticleComponent>()
}

enum class ParticleType {
    RAIN,
    FIRE,
    SHOOTING_STAR,
}
