package io.bennyoe.systems

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.Filter
import com.badlogic.gdx.physics.box2d.World
import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.quillraven.fleks.IntervalSystem
import com.github.quillraven.fleks.World.Companion.inject
import io.bennyoe.config.EntityCategory
import io.bennyoe.config.GameConstants.UNIT_SCALE
import io.bennyoe.event.MapChangedEvent
import io.bennyoe.lightEngine.core.LightEffectType
import io.bennyoe.lightEngine.core.Scene2dLightEngine
import ktx.log.logger
import ktx.math.times
import ktx.math.vec2
import ktx.tiled.layer
import ktx.tiled.x
import ktx.tiled.y

class LightSystem(
    private val stage: Stage = inject("stage"),
    private val phyWorld: World = inject("phyWorld"),
    private val lightEngine: Scene2dLightEngine = inject("lightEngine"),
) : IntervalSystem(),
    EventListener {
    override fun onTick() = Unit

    override fun handle(event: Event?): Boolean {
        when (event) {
            is MapChangedEvent -> {
                setupLights()
                val lightsLayer = event.map.layer("lights")
                lightsLayer.objects.forEach { light ->
                    val color = light.properties.get("color") as Color
                    val type = LightType.entries[(light.properties.get("type") as Int)]
                    val effect = LightEffectType.entries[(light.properties.get("effect") as Int)]
                    val position = vec2(light.x, light.y)

                    createLight(type, position, color, effect)
                }
                return true
            }
        }
        return false
    }

    private fun setupLights() {
        lightEngine.setShaderAmbientLight(Color(0.2f, 0.28f, 0.6f, 1f))
//        lightEngine.setShaderAmbientLight(Color(1f, 1f, 6f, 1f))
        lightEngine.setBox2dLightAmbientLight(Color(0.2f, 0.28f, 0.6f, 1f))
        lightEngine.setDiffuseLight(true)
        lightEngine.setNormalInfluence(.6f)

        val dir =
            lightEngine.addDirectionalLight(
                Color(0.15f, 0.18f, 0.25f, 1f),
                45f,
                6f,
                15f,
            )
        dir.b2dLight.apply {
            setContactFilter(
                Filter().apply {
                    categoryBits = EntityCategory.LIGHT.bit
                    maskBits = EntityCategory.GROUND.bit
                },
            )
        }
    }

    private fun createLight(
        type: LightType,
        position: Vector2,
        color: Color,
        effect: LightEffectType? = null,
    ) {
        when (type) {
            LightType.POINT_LIGHT -> {
                val light =
                    lightEngine.addPointLight(
                        position * UNIT_SCALE,
                        color,
                        6f,
                        7f,
                    )
                light.effect = effect

                light.b2dLight.apply {
                    setContactFilter(
                        Filter().apply {
                            categoryBits = EntityCategory.LIGHT.bit
                            maskBits = EntityCategory.GROUND.bit
                        },
                    )
                }
            }

            LightType.SPOT_LIGHT -> {
            }
        }
    }

    companion object {
        val logger = logger<LightSystem>()
    }
}

enum class LightType {
    POINT_LIGHT,
    SPOT_LIGHT,
}
