package io.bennyoe.event

import com.badlogic.gdx.maps.tiled.TiledMap
import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.Stage

fun Stage.fire(event: Event) {
    this.root.fire(event)
}

sealed interface GameEvent

// --- Map Events ---
data class MapChangedEvent(
    val map: TiledMap,
) : Event(),
    GameEvent

// --- Audio Events ---
sealed interface AudioEvent : GameEvent {
    val soundFile: String
}

data class AttackSoundEvent(
    override val soundFile: String = "sound/sword.mp3",
) : Event(),
    AudioEvent

data class WalkSoundEvent(
    override val soundFile: String = "sound/footsteps.mp3",
) : Event(),
    AudioEvent

data class WalkSoundStopEvent(
    override val soundFile: String = "sound/footsteps.mp3",
) : Event(),
    AudioEvent

data class HitSoundEvent(
    override val soundFile: String = "sound/hit.mp3",
) : Event(),
    AudioEvent
