package io.bennyoe.systems

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import com.github.quillraven.fleks.World.Companion.inject
import io.bennyoe.components.ImageComponent
import io.bennyoe.components.LightComponent
import io.bennyoe.components.PhysicComponent
import io.bennyoe.components.PlayerComponent
import io.bennyoe.event.MapChangedEvent
import io.bennyoe.lightEngine.core.GameLight
import io.bennyoe.lightEngine.core.Scene2dLightEngine
import ktx.log.logger
import ktx.math.times
import ktx.math.vec2

class LightSystem(
    private val lightEngine: Scene2dLightEngine = inject("lightEngine"),
) : IteratingSystem(family { all(LightComponent, ImageComponent, PlayerComponent, PhysicComponent) }),
    EventListener {
    override fun onTickEntity(entity: Entity) {
        val lightCmp = entity[LightComponent]
        val imageCmp = entity[ImageComponent]
        val physicCmp = entity[PhysicComponent]

        if (lightCmp.gameLight is GameLight.Spot) {
            val spotLight = lightCmp.gameLight
            spotLight.direction = if (imageCmp.flipImage) 180f else 0f
            spotLight.position = vec2(physicCmp.body.position.x, physicCmp.body.position.y)
        }
    }

    override fun handle(event: Event?): Boolean {
        when (event) {
            is MapChangedEvent -> {
                setupLights()
                return true
            }
        }
        return false
    }

    private fun setupLights() {
        lightEngine.updateShaderAmbientColor(Color(0.2f, 0.28f, 0.6f, 1f))
        lightEngine.setBox2dAmbientLight(Color(0.2f, 0.28f, 0.6f, 1f))
        lightEngine.setDiffuseLight(true)
        lightEngine.setNormalInfluence(1f)
        lightEngine.setSpecularIntensity(.2f)
        lightEngine.setSpecularRemap(0.0f, 0.2f)

        val dir =
            lightEngine.addDirectionalLight(
                Color(0.15f, 0.18f, 0.25f, .6f),
                45f,
                6f,
                15f,
            )
    }

    companion object {
        val logger = logger<LightSystem>()
    }
}

enum class LightType {
    POINT_LIGHT,
    SPOT_LIGHT,
}
