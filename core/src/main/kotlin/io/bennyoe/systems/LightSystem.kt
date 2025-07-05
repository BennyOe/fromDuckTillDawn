package io.bennyoe.systems

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.physics.box2d.Filter
import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
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
                    val type = LightType.entries[(light.properties.get("type") as Int)]
                    val position = vec2(light.x, light.y)
                    val color = light.properties.get("color") as Color
                    val initialIntensity = light.properties.get("initialIntensity") as Float? ?: 1f
                    val b2dDistance = light.properties.get("distance") as Float? ?: 1f
                    val falloffProfile = light.properties.get("falloffProfile") as Float? ?: 0.5f
                    val shaderIntensityMultiplier = light.properties.get("shaderIntensityMultiplier") as Float? ?: 0.5f

                    // spotlight specific
                    val direction = light.properties.get("direction") as Float? ?: -90f
                    val coneDegree = light.properties.get("coneDegree") as Float? ?: 50f

                    val effect = (light.properties.get("effect") as? Int)?.let { LightEffectType.entries[it] }

                    when (type) {
                        LightType.POINT_LIGHT -> {
                            val pointLight =
                                lightEngine.addPointLight(
                                    position * UNIT_SCALE,
                                    color,
                                    initialIntensity,
                                    b2dDistance,
                                    falloffProfile,
                                    shaderIntensityMultiplier,
                                )
                            pointLight.effect = effect

                            pointLight.b2dLight.apply {
                                setContactFilter(
                                    Filter().apply {
                                        categoryBits = EntityCategory.LIGHT.bit
                                        maskBits = EntityCategory.GROUND.bit
                                    },
                                )
                            }
                        }

                        LightType.SPOT_LIGHT -> {
                            val spotLight =
                                lightEngine.addSpotLight(
                                    position * UNIT_SCALE,
                                    color,
                                    direction,
                                    coneDegree,
                                    initialIntensity,
                                    b2dDistance,
                                    falloffProfile,
                                    shaderIntensityMultiplier,
                                )
                            spotLight.effect = effect

                            spotLight.b2dLight.apply {
                                setContactFilter(
                                    Filter().apply {
                                        categoryBits = EntityCategory.LIGHT.bit
                                        maskBits = EntityCategory.GROUND.bit
                                    },
                                )
                            }
                        }
                    }
                }
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
        dir.b2dLight.apply {
            setContactFilter(
                Filter().apply {
                    categoryBits = EntityCategory.LIGHT.bit
                    maskBits = EntityCategory.GROUND.bit
                },
            )
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
