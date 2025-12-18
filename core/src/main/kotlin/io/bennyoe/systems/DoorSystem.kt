package io.bennyoe.systems

import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import com.github.quillraven.fleks.World.Companion.inject
import io.bennyoe.components.DoorComponent
import io.bennyoe.components.PhysicComponent
import io.bennyoe.components.TransformComponent
import io.bennyoe.event.DoorEvent
import io.bennyoe.event.PlaySoundEvent
import io.bennyoe.event.fire
import io.bennyoe.systems.audio.SoundType
import ktx.log.logger

private const val DOOR_SPEED = 0.8f

class DoorSystem(
    private val stage: Stage = inject("stage"),
) : IteratingSystem(family { all(DoorComponent, TransformComponent, PhysicComponent) }),
    EventListener {
    override fun handle(event: Event): Boolean {
        when (event) {
            is DoorEvent -> {
                val doorEntity = event.targetDoorEntity
                val doorCmp = doorEntity[DoorComponent]
                val transformCmp = doorEntity[TransformComponent]

                if (doorCmp.initialPosition == null) {
                    doorCmp.initialPosition = transformCmp.position.cpy()
                }

                doorCmp.isOpen = !doorCmp.isOpen
                val init = doorCmp.initialPosition!!
                doorCmp.targetY = if (doorCmp.isOpen) init.y - transformCmp.height - 2f else init.y

                stage.fire(PlaySoundEvent(doorEntity, SoundType.LAUGH, 1f))
                return true
            }
        }
        return false
    }

    override fun onTickEntity(entity: Entity) {
        val doorCmp = entity[DoorComponent]
        val transformCmp = entity[TransformComponent]
        val physicCmp = entity[PhysicComponent]

        val initialPos = doorCmp.initialPosition ?: return

        // Determine the target Y position based on whether the door is open or closed.
        doorCmp.targetY =
            if (doorCmp.isOpen) {
                initialPos.y - transformCmp.height - 2f
            } else {
                initialPos.y
            }

        val currentPos = physicCmp.body.position

        val newY = MathUtils.lerp(currentPos.y, doorCmp.targetY!!, deltaTime * DOOR_SPEED)

        physicCmp.body.setTransform(currentPos.x, newY, physicCmp.body.angle)
    }

    companion object {
        val logger = logger<DoorSystem>()
    }
}
