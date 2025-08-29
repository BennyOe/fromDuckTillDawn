package io.bennyoe.systems.entitySpawn

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.maps.MapLayer
import com.badlogic.gdx.math.Vector2
import io.bennyoe.config.GameConstants
import io.bennyoe.lightEngine.core.LightEffectType
import io.bennyoe.lightEngine.core.Scene2dLightEngine
import ktx.math.times
import ktx.math.vec2
import ktx.tiled.x
import ktx.tiled.y

class LightSpawner(
    val lightEngine: Scene2dLightEngine,
) {
    fun spawnFromMap(lightsLayer: MapLayer) {
        lightsLayer.objects?.forEach { light ->
            val type = LightType.entries[(light.properties.get("type") as Int)]
            val position = vec2(light.x, light.y)
            val color = light.properties.get("color") as Color
            val initialIntensity = light.properties.get("initialIntensity") as Float? ?: 1f
            val b2dDistance = light.properties.get("distance") as Float? ?: 1f
            val falloffProfile = light.properties.get("falloffProfile") as Float? ?: 0.5f
            val shaderIntensityMultiplier = light.properties.get("shaderIntensityMultiplier") as Float? ?: 0.5f
            val isManaged = light.properties.get("isManaged") as Boolean? ?: true

            // spotlight specific
            val direction = light.properties.get("direction") as Float? ?: -90f
            val coneDegree = light.properties.get("coneDegree") as Float? ?: 50f

            val effect = (light.properties.get("effect") as? Int)?.let { LightEffectType.entries[it] }

            createLight(
                type,
                position,
                color,
                initialIntensity,
                b2dDistance,
                falloffProfile,
                shaderIntensityMultiplier,
                effect,
                direction,
                coneDegree,
                isManaged,
            )
        }
    }

    private fun createLight(
        type: LightType,
        position: Vector2,
        color: Color,
        initialIntensity: Float,
        b2dDistance: Float,
        falloffProfile: Float,
        shaderIntensityMultiplier: Float,
        effect: LightEffectType?,
        direction: Float,
        coneDegree: Float,
        isManaged: Boolean,
    ) {
        when (type) {
            LightType.POINT_LIGHT -> {
                val pointLight =
                    lightEngine.addPointLight(
                        position * GameConstants.UNIT_SCALE,
                        color,
                        initialIntensity,
                        b2dDistance,
                        falloffProfile,
                        shaderIntensityMultiplier,
                        isManaged = isManaged,
                    )
                pointLight.effect = effect
                pointLight.setOn(true)
            }

            LightType.SPOT_LIGHT -> {
                val spotLight =
                    lightEngine.addSpotLight(
                        position * GameConstants.UNIT_SCALE,
                        color,
                        direction,
                        coneDegree,
                        initialIntensity,
                        b2dDistance,
                        falloffProfile,
                        shaderIntensityMultiplier,
                        isManaged = isManaged,
                    )
                spotLight.effect = effect
            }
        }
    }
}

enum class LightType {
    POINT_LIGHT,
    SPOT_LIGHT,
}
