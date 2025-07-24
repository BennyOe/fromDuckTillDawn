package io.bennyoe.systems.audio

import com.badlogic.gdx.math.MathUtils
import com.github.quillraven.fleks.IntervalSystem
import de.pottgames.tuningfork.EaxReverb
import de.pottgames.tuningfork.SoundEffect
import de.pottgames.tuningfork.SoundSource
import io.bennyoe.components.PhysicComponent
import io.bennyoe.components.PlayerComponent
import io.bennyoe.components.audio.AudioComponent
import io.bennyoe.components.audio.ReverbZoneComponent
import io.bennyoe.components.audio.ReverbZoneContactComponent
import ktx.log.logger

private const val REVERB_TAIL = 5f

/**
 * A Fleks [IntervalSystem] responsible for applying real-time reverb effects to [SoundSource]s
 * based on the player's current location within [ReverbZone]s.
 *
 * The system manages a single active [SoundEffect] (reverb) that is dynamically assigned depending
 * on the player's contact with zones. When the player enters a zone, a corresponding effect is fetched
 * and applied to all registered sound sources and buffered audio components.
 *
 * ## Features:
 * - Dynamically switches reverb effect based on zone preset and intensity
 * - Crossfades reverb intensity (dry/wet mix) when staying in the same zone
 * - Applies tail reverb to preserve natural fade-out when switching presets
 * - Effect is automatically removed when the player leaves all reverb zones
 * - Supports registering and unregistering dynamic [SoundSource]s (e.g. for SFX, ambience, etc.)
 *
 * ## Lifecycle:
 * - On each tick, checks the player's active [ReverbZoneContactComponent] and updates effects accordingly
 * - Detached effects are retained as "tails" for a short time before being disposed
 * - On system disposal, all effects are detached and cleaned up
 *
 * @see AudioComponent for buffered sources that also receive effects
 * @see registerSource for adding new real-time sources to the effect
 * @see unregisterSource for cleaning up finished sources
 */
class ReverbSystem : IntervalSystem() {
    private val detachedEffects: MutableList<TailReverb> = mutableListOf()
    private var attachEffectToNewSources = false
    private var activeEffect: SoundEffect? = null
    private var activePreset: String? = null
    private var currentWet = 1f
    private val playerEntity by lazy { world.family { all(PlayerComponent, PhysicComponent) }.first() }
    private val activeSources = mutableSetOf<SoundSource>()

    private data class TailReverb(
        val effect: SoundEffect,
        var ttl: Float = REVERB_TAIL,
    )

    fun registerSource(src: SoundSource) {
        activeSources += src
        applyReverbToNewSource(src)
    }

    fun unregisterSource(src: SoundSource) {
        activeSources -= src
    }

    override fun onTick() {
        updateReverbZones()
    }

    private fun updateReverbZones() {
        val currentZone = playerEntity[ReverbZoneContactComponent].activeZone
        when {
            currentZone == null && activeEffect != null -> handleZoneExit()
            currentZone != null && currentZone.presetName != activePreset -> handleZoneChange(currentZone)
            currentZone != null -> updateWetDryMix(currentZone.intensity.coerceIn(0f, 1f))
        }
        cleanupReverb()
    }

    private fun handleZoneExit() {
        activeEffect?.let {
            detachEffectFromAllSources(it)
            detachedEffects += TailReverb(it)
        }
        activeEffect = null
        activePreset = null
        attachEffectToNewSources = false
        currentWet = 1f
    }

    private fun handleZoneChange(zone: ReverbZoneComponent) {
        activeEffect?.let {
            detachEffectFromAllSources(it)
            detachedEffects += TailReverb(it)
        }

        getReverb(zone.presetName)?.let { newFx ->
            activeEffect = newFx
            activePreset = zone.presetName
            currentWet = zone.intensity.coerceIn(0f, 1f)
            attachEffectToNewSources = true
            attachEffectToAllSources(newFx, currentWet)
        }
    }

    private fun updateWetDryMix(newWet: Float) {
        if (!MathUtils.isEqual(newWet, currentWet)) {
            currentWet = newWet
            val dry = 1f - newWet
            activeSources.forEach { it.setFilter(dry, dry) }
            world.family { all(AudioComponent) }.forEach { e ->
                e[AudioComponent].bufferedSoundSource?.setFilter(dry, dry)
            }
        }
    }

    private fun attachEffectToAllSources(
        effect: SoundEffect,
        wet: Float,
    ) {
        val dry = 1f - wet
        activeSources.forEach {
            it.setFilter(dry, dry)
            it.attachEffect(effect, wet, wet)
        }
        world.family { all(AudioComponent) }.forEach { e ->
            e[AudioComponent].bufferedSoundSource?.attachEffect(effect, wet, wet)
            e[AudioComponent].bufferedSoundSource?.setFilter(dry, dry)
        }
    }

    fun applyReverbToNewSource(src: SoundSource) {
        if (!attachEffectToNewSources || activeEffect == null) return
        val dry = 1f - currentWet
        src.setFilter(dry, dry)
        src.attachEffect(activeEffect!!, currentWet, currentWet)
    }

    private fun cleanupReverb() {
        val it = detachedEffects.iterator()
        while (it.hasNext()) {
            val tail = it.next()
            tail.ttl -= deltaTime
            if (tail.ttl <= 0f) {
                detachAndDisposeEffect(tail.effect)
                it.remove()
            }
        }
    }

    private fun detachAndDisposeEffect(effect: SoundEffect) {
        detachEffectFromAllSources(effect)
        effect.dispose()
    }

    private fun detachEffectFromAllSources(effect: SoundEffect) {
        activeSources.forEach { it.detachEffect(effect) }
        world.family { all(AudioComponent) }.forEach { e ->
            e[AudioComponent].bufferedSoundSource?.detachEffect(effect)
        }
    }

    private fun getReverb(presetName: String): SoundEffect? {
        return when (presetName.uppercase()) {
            "CAVE" -> SoundEffect(EaxReverb.cave())
            "FOREST" -> SoundEffect(EaxReverb.forest())
            "ARENA" -> SoundEffect(EaxReverb.arena())
            "HANGER" -> SoundEffect(EaxReverb.hangar())
            "STONEROOM" -> SoundEffect(EaxReverb.stoneRoom())
            else -> {
                logger<ReverbSystem>().error { "Unknown EaxReverb preset: '$presetName'" }
                return null
            }
        }
    }

    override fun onDispose() {
        activeSources.forEach { it.detachAllEffects() }
        super.onDispose()
    }
}
