package io.bennyoe.components.audio

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

class ReverbZoneContactComponent : Component<ReverbZoneContactComponent> {
    private var reverbZoneContacts: Int = 0
    var activeZone: ReverbZoneComponent? = null

    fun increaseContact(zone: ReverbZoneComponent): Int {
        reverbZoneContacts += 1
        if (reverbZoneContacts > 0) {
            activeZone = zone
        }
        return reverbZoneContacts
    }

    fun decreaseContact(zone: ReverbZoneComponent): Int {
        if (reverbZoneContacts > 0) {
            reverbZoneContacts -= 1
            if (reverbZoneContacts == 0) {
                activeZone = null
            }
        }
        return reverbZoneContacts
    }

    override fun type() = ReverbZoneContactComponent

    companion object : ComponentType<ReverbZoneContactComponent>()
}
