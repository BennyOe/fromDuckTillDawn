package io.bennyoe.event

import com.badlogic.gdx.maps.tiled.TiledMap
import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.Stage
import io.bennyoe.assets.SoundAssets
import io.bennyoe.utility.FloorType

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
sealed interface AudioEvent : GameEvent

data class PlaySoundEvent(
    val sound: SoundAssets,
    val volume: Float,
) : Event(),
    AudioEvent

data class PlayLoopingSoundEvent(
    val sound: SoundAssets,
    val loopId: String,
    val volume: Float,
    val floorType: FloorType?,
) : Event(),
    AudioEvent

data class StopLoopingSoundEvent(
    val loopId: String,
) : Event(),
    AudioEvent
