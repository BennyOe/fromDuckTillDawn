package io.bennyoe.systems.audio

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import io.bennyoe.components.IsDiving
import io.bennyoe.components.PlayerComponent
import io.bennyoe.components.StateComponent

class UnderWaterSoundSystem : IteratingSystem(family { all(PlayerComponent, StateComponent) }) {
    private val reverb = world.system<ReverbSystem>()
    private var underwaterSound: FadingSound? = null

    override fun onTickEntity(entity: Entity) {
        if (entity has IsDiving) {
            reverb.setGlobalFilters(1f, 0.002f)
            if (underwaterSound == null || underwaterSound!!.isStopped()) {
                underwaterSound =
                    FadingSound(
                        soundPath = "sound/ambience/underwater.mp3",
                        targetVolume = 1f,
                        fadeInDuration = 1.5f,
                        fadeOutDuration = 1.5f,
                        reverbSystem = reverb,
                    )
            }
        } else {
            // Player is not underwater
            reverb.setGlobalFilters(1f, 1f) // Reset filters

            // Fade out and stop the underwater sound if it is playing
            underwaterSound?.let {
                if (!it.isStopped()) {
                    it.fadeOut()
                }
            }
            // Once faded out, the update loop in FadingSound will handle disposal
        }

        // Update the sound's fade logic
        underwaterSound?.update(deltaTime)
        if (underwaterSound?.isStopped() == true) {
            underwaterSound = null
        }
    }

    override fun onDispose() {
        underwaterSound?.stop()
        super.onDispose()
    }
}
