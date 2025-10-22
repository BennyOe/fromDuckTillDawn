package io.bennyoe.systems

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import com.github.quillraven.fleks.World.Companion.inject
import io.bennyoe.components.CloudComponent
import io.bennyoe.components.DisabledComponent
import io.bennyoe.components.GameStateComponent
import io.bennyoe.components.ImageComponent
import io.bennyoe.components.TransformComponent
import io.bennyoe.components.Weather
import io.bennyoe.systems.render.ZIndex
import io.bennyoe.utility.getViewportDimensions
import ktx.collections.GdxArray
import ktx.math.vec2

class CloudSystem(
    private val stage: Stage = inject("stage"),
    private val cloudsAtlas: TextureAtlas = inject("cloudsAtlas"),
    private val rainCloudsAtlas: TextureAtlas = inject("rainCloudsAtlas"),
) : IteratingSystem(
        family {
            all(TransformComponent, ImageComponent, CloudComponent)
            none(DisabledComponent)
        },
    ),
    PausableSystem {
    private val gameStateCmp by lazy { world.family { all(GameStateComponent) }.first()[GameStateComponent] }
    private val cloudPool = GdxArray<Entity>()
    private val camera = stage.camera as OrthographicCamera
    private var spawnTimer = 0f
    private var previousWeather: Weather? = null

    private val numClouds = 128

    override fun onTick() {
        super.onTick()
        if (gameStateCmp.weather == Weather.CLEAR) return

        spawnTimer -= deltaTime
        if ((spawnTimer <= 0f || previousWeather != gameStateCmp.weather) && cloudPool.notEmpty()) {
            spawnCloud(cloudPool.pop())
            spawnTimer = gameStateCmp.weather.cloudSpawnSpeed
        }
        previousWeather = gameStateCmp.weather
    }

    override fun onTickEntity(entity: Entity) {
        val transformCmp = entity[TransformComponent]
        val cloudCmp = entity[CloudComponent]
        val imageCmp = entity[ImageComponent]

        transformCmp.position.x += cloudCmp.speed * cloudCmp.parallaxFactor * deltaTime

        val viewportRight = camera.position.x + camera.viewportWidth * camera.zoom / 2f
        if (transformCmp.position.x > viewportRight + imageCmp.image.width) {
            entity.configure { it += DisabledComponent }
            imageCmp.image.isVisible = false
            cloudPool.add(entity)
        }
    }

    override fun onDispose() {
        cloudPool.clear()
        super.onDispose()
    }

    fun initializeCloudPool() {
        println("CloudPool initialized with $numClouds entities")

        if (cloudPool.notEmpty()) return

        repeat(numClouds) {
            val entity =
                world.entity {
                    it += CloudComponent()
                    it += TransformComponent(position = vec2(0f, 0f), width = 1f, height = 1f)
                    val image = ImageComponent(stage, zIndex = ZIndex.CLOUDS.value + it.id)
                    image.image = Image()
                    image.image.isVisible = false
                    it += image
                    it += DisabledComponent
                }
            cloudPool.add(entity)
        }
    }

    private fun spawnCloud(entity: Entity) {
        val region = getCloudRegion()
        val weather = gameStateCmp.weather

        val aspect = region.originalHeight.toFloat() / region.originalWidth.toFloat()
        println("Spawned cloud with region: ${region.name}")

        val parallax = MathUtils.random(0.4f, 1.0f)

        entity.configure {
            val cloudCmp = it[CloudComponent]
            cloudCmp.speed = MathUtils.random(1.5f, 3.0f)
            cloudCmp.parallaxFactor = parallax

            val transformCmp = it[TransformComponent]
            val width = MathUtils.random(weather.minSize, weather.maxSize) * parallax
            transformCmp.width = width
            transformCmp.height = width * aspect

            val viewportDimensions = getViewportDimensions(stage)

            val x = viewportDimensions.left - transformCmp.width

            val yMin = viewportDimensions.bottom + (viewportDimensions.top - viewportDimensions.bottom) * weather.minHeightMultiplier
            val yMax = viewportDimensions.bottom + (viewportDimensions.top - viewportDimensions.bottom) * 0.90f
            val yCenter = MathUtils.random(yMin, yMax)

            transformCmp.position.set(x, yCenter - transformCmp.height * 0.5f)

            val imgCmp = it[ImageComponent]
            imgCmp.zIndex = MathUtils.random(weather.minZIndex, weather.maxZIndex) + it.id
            val trd = imgCmp.image.drawable as? TextureRegionDrawable
            if (trd != null) {
                trd.region = region
            } else {
                imgCmp.image.drawable = TextureRegionDrawable(region)
            }
            imgCmp.image.color.a = MathUtils.random(weather.minImageAlpha, 1.0f)
            imgCmp.image.isVisible = true

            it -= DisabledComponent
        }
    }

    private fun getCloudRegion(): TextureAtlas.AtlasRegion =
        when (gameStateCmp.weather) {
            Weather.RAIN -> rainCloudsAtlas.findRegion("rainCloud${MathUtils.random(1, 6)}")
            else -> cloudsAtlas.findRegion("cloud${MathUtils.random(1, 8)}")
        }
}
