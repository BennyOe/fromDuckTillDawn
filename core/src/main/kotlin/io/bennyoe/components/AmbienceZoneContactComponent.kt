package io.bennyoe.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import io.bennyoe.components.audio.AmbienceSoundComponent

class AmbienceZoneContactComponent : Component<AmbienceZoneContactComponent> {
    private val activeZones = mutableListOf<Pair<AmbienceSoundComponent, IsIndoorComponent>>()

    fun addContact(
        ambience: AmbienceSoundComponent,
        indoor: IsIndoorComponent,
    ) {
        // Add the new zone only if it's not already in the list to avoid duplicates.
        if (!activeZones.contains(ambience to indoor)) {
            activeZones.add(ambience to indoor)
        }
    }

    fun removeContact(
        ambience: AmbienceSoundComponent,
        indoor: IsIndoorComponent,
    ) {
        activeZones.remove(ambience to indoor)
    }

    fun getActiveZone(): Pair<AmbienceSoundComponent, IsIndoorComponent>? = activeZones.lastOrNull()

    override fun type() = AmbienceZoneContactComponent

    companion object : ComponentType<AmbienceZoneContactComponent>()
}
