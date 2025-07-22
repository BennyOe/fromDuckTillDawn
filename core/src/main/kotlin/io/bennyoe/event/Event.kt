package io.bennyoe.event

import com.badlogic.gdx.maps.tiled.TiledMap
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.quillraven.fleks.Entity
import io.bennyoe.service.SoundType
import io.bennyoe.utility.FloorType

// helper for convenience
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
    val entity: Entity,
    val soundType: SoundType,
    val volume: Float,
    val position: Vector2? = null,
    val floorType: FloorType? = null,
) : Event(),
    AudioEvent

data class PlayLoopingSoundEvent(
    val entity: Entity,
    val soundType: SoundType,
    val volume: Float,
    val position: Vector2? = null,
    val floorType: FloorType? = null,
) : Event(),
    AudioEvent

data class StopLoopingSoundEvent(
    val loopId: SoundType,
) : Event(),
    AudioEvent

data class StreamSoundEvent(
    val entity: Entity,
    val sound: String,
    val volume: Float,
    val position: Vector2? = null,
) : Event(),
    AudioEvent
