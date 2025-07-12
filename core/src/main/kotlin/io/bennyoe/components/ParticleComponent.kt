package io.bennyoe.components

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.g2d.ParticleEffect
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World

class ParticleComponent(
    val particleFile: FileHandle,
    val scaleFactor: Float = 1f,
    val motionScaleFactor: Float = 1f,
    var active: Boolean = true,
    var looping: Boolean = true,
    var offsetX: Float = 0f,
    var offsetY: Float = 0f,
) : Component<ParticleComponent> {
    lateinit var particleEffect: ParticleEffect
    var timer: Float = 0f
    var duration: Float = 0f

    override fun type() = ParticleComponent

    override fun World.onAdd(entity: Entity) {
        val particlesAtlas: TextureAtlas = inject("particlesAtlas")
        particleEffect = ParticleEffect()
        particleEffect.load(particleFile, particlesAtlas)
        particleEffect.scaleEffect(scaleFactor, motionScaleFactor)
        particleEffect.start()

        if (!looping) {
            duration = 64f / 1000f // Convert ms to seconds
        }
    }

    override fun World.onRemove(entity: Entity) {
        particleEffect.dispose()
    }

    companion object : ComponentType<ParticleComponent>()
}
