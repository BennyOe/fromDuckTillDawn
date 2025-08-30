package io.bennyoe.actors

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.ParticleEffect
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.utils.Disposable

class ParticleActor(
    val effect: ParticleEffect,
) : Actor(),
    Disposable {
    init {
        // Set the actor's position when the effect is created
        effect.setPosition(x, y)
    }

    override fun act(delta: Float) {
        super.act(delta)
        effect.update(delta)
    }

    override fun draw(
        batch: Batch,
        parentAlpha: Float,
    ) {
        // The effect knows its own position, so we just draw it.
        effect.draw(batch)
    }

    // We need a way to update the position from the outside
    override fun setPosition(
        x: Float,
        y: Float,
    ) {
        super.setPosition(x, y)
        effect.setPosition(x, y)
    }

    override fun dispose() {
        effect.dispose()
    }
}
