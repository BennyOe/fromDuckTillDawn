package io.bennyoe.systems.entitySpawn

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.maps.MapLayer
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.github.quillraven.fleks.World
import io.bennyoe.assets.SoundAssets
import io.bennyoe.components.CrowComponent
import io.bennyoe.components.DisabledComponent
import io.bennyoe.components.ImageComponent
import io.bennyoe.components.LightComponent
import io.bennyoe.components.LightningComponent
import io.bennyoe.components.ParticleComponent
import io.bennyoe.components.ParticleType
import io.bennyoe.components.RainComponent
import io.bennyoe.components.ShaderRenderingComponent
import io.bennyoe.components.SkyComponent
import io.bennyoe.components.SkyComponentType
import io.bennyoe.components.TransformComponent
import io.bennyoe.components.animation.AnimationComponent
import io.bennyoe.components.animation.AnimationModel
import io.bennyoe.components.animation.CrowAnimation
import io.bennyoe.components.audio.AudioComponent
import io.bennyoe.components.audio.SoundProfileComponent
import io.bennyoe.config.GameConstants.UNIT_SCALE
import io.bennyoe.config.GameConstants.WORLD_HEIGHT
import io.bennyoe.config.GameConstants.WORLD_WIDTH
import io.bennyoe.lightEngine.core.LightEffectType
import io.bennyoe.lightEngine.core.Scene2dLightEngine
import io.bennyoe.systems.audio.SoundProfile
import io.bennyoe.systems.audio.SoundType
import io.bennyoe.systems.render.ZIndex
import io.bennyoe.utility.setupShader
import ktx.math.vec2
import ktx.tiled.type
import ktx.tiled.x
import ktx.tiled.y

class SkySpawner(
    val world: World,
    val lightEngine: Scene2dLightEngine,
    val stage: Stage,
    val worldObjectsAtlas: TextureAtlas,
) {
    fun spawnSkyObjects(
        skyObjectsLayer: MapLayer,
        layerZIndex: Int,
    ) {
        // spawn rain
        world.entity {
            it += TransformComponent(vec2(-WORLD_WIDTH, WORLD_HEIGHT + 10f), WORLD_WIDTH, WORLD_HEIGHT)
            val particle =
                ParticleComponent(
                    particleFile = Gdx.files.internal("particles/rain.p"),
                    scaleFactor = .1f,
                    motionScaleFactor = .3f,
                    looping = true,
                    stage = stage,
                    zIndex = ZIndex.PARTICLES.value,
                    enabled = false,
                    type = ParticleType.RAIN,
                )
            it += particle
            it += RainComponent
        }

        skyObjectsLayer.objects?.forEach { skyObject ->
            val zIndex = skyObject.properties.get("zIndex", Int::class.java) ?: 0
            val width = (stage.camera as OrthographicCamera).viewportWidth
            val height = (stage.camera as OrthographicCamera).viewportHeight
            when (skyObject.type) {
                "lightning" -> {
                    val position = vec2(skyObject.x * UNIT_SCALE, skyObject.y * UNIT_SCALE)
                    val isManaged = skyObject.properties.get("isManaged") as Boolean? ?: true
                    val effect = (skyObject.properties.get("effect") as? Int)?.let { LightEffectType.entries[it] }
                    val b2dDistance = skyObject.properties.get("distance") as Float? ?: 1f
                    val initialIntensity = skyObject.properties.get("initialIntensity") as Float? ?: 1f
                    val color = skyObject.properties.get("color") as Color
                    val falloffProfile = skyObject.properties.get("falloffProfile") as Float? ?: 0.5f
                    val shaderIntensityMultiplier = skyObject.properties.get("shaderIntensityMultiplier") as Float? ?: 0.5f
                    world.entity {
                        it += LightningComponent
                        val lightning =
                            LightComponent(
                                lightEngine.addPointLight(
                                    position = position,
                                    color = color,
                                    b2dDistance = b2dDistance,
                                    initialIntensity = initialIntensity,
                                    falloffProfile = falloffProfile,
                                    shaderIntensityMultiplier = shaderIntensityMultiplier,
                                    isManaged = isManaged,
                                ),
                            )
                        lightning.gameLight.effect = effect
                        lightning.gameLight.effectParams.lightningMinDelay = 8f
                        lightning.gameLight.effectParams.lightningMaxDelay = 30f
                        it += lightning
                    }
                }

                "shootingStar" -> {
                    world.entity {
                        it += TransformComponent(vec2(0f, 0f), width, height)
                        val particle =
                            ParticleComponent(
                                particleFile = Gdx.files.internal("particles/shootingStar.p"),
                                scaleFactor = 0.1f,
                                motionScaleFactor = 0.2f,
                                looping = true,
                                stage = stage,
                                zIndex = layerZIndex + zIndex,
                                type = ParticleType.SHOOTING_STAR,
                            )
                        it += particle
                        it += SkyComponent(SkyComponentType.SHOOTING_STAR)
                    }
                }

                "crow" -> {
                    world.entity {
                        val position = vec2(100f, skyObject.y * UNIT_SCALE)
                        it += TransformComponent(position, 1.4f, 1.4f)
                        val imageCmp = ImageComponent(stage, zIndex = layerZIndex + zIndex)
                        imageCmp.image = Image()
                        it += imageCmp
                        it += ShaderRenderingComponent()
                        it +=
                            AudioComponent(
                                soundAttenuationFactor = 0.5f,
                                soundAttenuationMinDistance = 10f,
                                soundAttenuationMaxDistance = 20f,
                                soundVolume = 1f,
                                soundType = SoundType.CROW,
                            )
                        it +=
                            SoundProfileComponent(
                                SoundProfile(simpleSounds = mapOf(SoundType.CROW to listOf(SoundAssets.CROW))),
                            )
                        val animation = AnimationComponent()
                        animation.animationModel = AnimationModel.CROW
                        animation.nextAnimation(CrowAnimation.FLY)
                        animation.animationSoundTriggers =
                            mapOf(CrowAnimation.FLY to mapOf(11 to SoundType.CROW))
                        it += animation
                        it += CrowComponent
                        it += DisabledComponent
                    }
                }

                "sky", "stars" -> {
                    world.entity {
                        val imageCmp = ImageComponent(stage, zIndex = layerZIndex + zIndex)
                        val imageName = skyObject.properties.get("image") as String
                        imageCmp.image = Image(worldObjectsAtlas.findRegion(imageName))

                        it += imageCmp
                        it += TransformComponent(vec2(0f, 0f), width, height)

                        val skyType = if (skyObject.type == "sky") SkyComponentType.SKY else SkyComponentType.STARS
                        it += SkyComponent(skyType)
                    }
                }

                "moon" -> {
                    world.entity {
                        it += SkyComponent(SkyComponentType.MOON)
                        val image = ImageComponent(stage, zIndex = layerZIndex + zIndex)
                        image.image = Image(worldObjectsAtlas.findRegion("moon2"))
                        it += image
                        val transform = TransformComponent(vec2(0f, 0f), 3f, 3f)
                        it += transform
                        val moonLight =
                            LightComponent(
                                lightEngine.addPointLight(
                                    position = transform.position,
                                    color = Color.WHITE,
                                    b2dDistance = 9f,
                                    isManaged = false,
                                ),
                            )
                        moonLight.gameLight.b2dLight.isXray = true
                        moonLight.gameLight.b2dLight.isStaticLight = false
                        it += moonLight
                        val shaderRenderingCmp = ShaderRenderingComponent()
                        shaderRenderingCmp.shader = setupShader("moon")
                        shaderRenderingCmp.uniforms.putAll(
                            mapOf(
                                "u_halo_color" to Vector3(1f, 1f, 1f),
                                "u_halo_radius" to 0.13f,
                                "u_halo_falloff" to 0.42f,
                                "u_halo_strength" to 0.4f,
                            ),
                        )
                        it += shaderRenderingCmp
                    }
                }

                "sun" -> {
                    world.entity {
                        it += SkyComponent(SkyComponentType.SUN)
                        val image = ImageComponent(stage, zIndex = layerZIndex + zIndex)
                        image.image = Image(worldObjectsAtlas.findRegion("sun2"))
                        it += image
                        val transform = TransformComponent(vec2(0f, 0f), 6f, 6f)
                        it += transform
                        val sunLight =
                            LightComponent(
                                lightEngine.addPointLight(
                                    position = transform.position,
                                    color = Color.WHITE,
                                    b2dDistance = 9f,
                                    isManaged = false,
                                ),
                            )
                        sunLight.gameLight.b2dLight.isStaticLight = true
                        sunLight.gameLight.b2dLight.isXray = true
                        it += sunLight
                        val shaderRenderingCmp = ShaderRenderingComponent()
                        shaderRenderingCmp.shader = setupShader("sun")
                        val region = worldObjectsAtlas.findRegion("noiseTexture")
                        val tex =
                            region.texture.apply {
                                setWrap(
                                    Texture.TextureWrap.Repeat,
                                    Texture.TextureWrap.Repeat,
                                )
                            }
                        shaderRenderingCmp.noiseTexture = tex
                        shaderRenderingCmp.uniforms.putAll(
                            mapOf(
                                "u_noiseOffset" to Vector2(region.u, region.v),
                                "u_noiseScale" to Vector2(region.u2 - region.u, region.v2 - region.v),
                                "u_sunsetCenter" to 17.25f,
                                "u_halfWidth" to 1.25f,
                                "u_tintStrength" to 0.6f,
                                "u_sunsetTint" to Vector3(1.0f, 0.5f, 0.2f),
                                "u_halo_color" to Vector3(1.0f, 0.6f, 0.2f),
                                "u_halo_radius" to 0.13f,
                                "u_halo_falloff" to 0.42f,
                                "u_halo_strength" to 1.0f,
                                "u_shimmer_strength" to 0.03f,
                                "u_shimmer_speed" to 0.2f,
                                "u_shimmer_scale" to 1.0f,
                                "u_bloom_threshold" to 0.97f,
                                "u_bloom_strength" to 4f,
                                "u_bloom_radius" to 129f,
                            ),
                        )
                        it += shaderRenderingCmp
                    }
                }

                else -> {
                    Unit
                }
            }
        }
    }
}
