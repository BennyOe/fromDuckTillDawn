package io.bennyoe.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import io.bennyoe.utility.SoundEffectEnum

class AudioZoneContactComponent : Component<AudioZoneContactComponent> {
    private val audioZoneContacts: MutableMap<SoundEffectEnum, Int> = mutableMapOf()
    var activeZone: AudioZoneComponent? = null

    fun increaseContact(zone: AudioZoneComponent): Int {
        val newCount = (audioZoneContacts[zone.effect] ?: 0) + 1
        audioZoneContacts[zone.effect] = newCount
        if (newCount > 0) {
            activeZone = zone
        }
        return newCount
    }

    fun decreaseContact(zone: AudioZoneComponent): Int {
        val current = audioZoneContacts[zone.effect] ?: return -1
        val newCount = current - 1
        if (newCount > 0) {
            audioZoneContacts[zone.effect] = newCount
        } else {
            audioZoneContacts.remove(zone.effect)
            activeZone = null
        }
        return newCount
    }

    fun getContactsFor(zone: AudioZoneComponent): Int = audioZoneContacts[zone.effect] ?: -1

    override fun type() = AudioZoneContactComponent

    companion object : ComponentType<AudioZoneContactComponent>()
}
