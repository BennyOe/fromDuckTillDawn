package io.bennyoe.systems.entitySpawn

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.maps.MapLayer
import com.badlogic.gdx.maps.MapObject
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Array
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.EntityCreateContext
import com.github.quillraven.fleks.World
import io.bennyoe.components.AnimationComponent
import io.bennyoe.components.ImageComponent
import io.bennyoe.components.ParallaxComponent
import io.bennyoe.components.TransformComponent
import io.bennyoe.config.GameConstants.UNIT_SCALE
import ktx.math.vec2
import ktx.tiled.height
import ktx.tiled.width
import ktx.tiled.x
import ktx.tiled.y

class BackgroundParallaxSpawner(
    val world: World,
    val stage: Stage,
    val animatesBgAtlas: TextureAtlas,
) {
    fun spawnParallaxBackgrounds(
        backgroundLayer: MapLayer,
        layerZIndex: Int,
    ) {
        val parallaxFactor = vec2(backgroundLayer.parallaxX, backgroundLayer.parallaxY)

        backgroundLayer.objects.forEach { background ->
            world.entity {
                val zIndex = background.properties.get("zIndex", Int::class.java) ?: 0

                var imageWidthInWorldUnits: Float
                var imageHeightInWorldUnits: Float

                val imageCmp = ImageComponent(stage, zIndex = layerZIndex + zIndex)

                if (background.properties.get("isAnimated") as? Boolean ?: false) {
                    val imageDimensions = setBackgroundAnimation(background, it, imageCmp)
                    imageWidthInWorldUnits = imageDimensions.x * UNIT_SCALE
                    imageHeightInWorldUnits = imageDimensions.y * UNIT_SCALE
                } else {
                    val texture =
                        Texture(Gdx.files.internal("images/backgrounds/${background.properties.get("image")}.png"))
                    imageCmp.image = Image(texture)

                    imageWidthInWorldUnits = texture.width * UNIT_SCALE
                    imageHeightInWorldUnits = texture.height * UNIT_SCALE
                }
                it += imageCmp

                it +=
                    ParallaxComponent(
                        parallaxFactor,
                        vec2(background.x * UNIT_SCALE, background.y * UNIT_SCALE),
                        background.width * UNIT_SCALE,
                        background.height * UNIT_SCALE,
                    )

                it +=
                    TransformComponent(
                        vec2(background.x * UNIT_SCALE, background.y * UNIT_SCALE),
                        imageWidthInWorldUnits,
                        imageHeightInWorldUnits,
                    )
            }
        }
    }

    private fun EntityCreateContext.setBackgroundAnimation(
        background: MapObject,
        entity: Entity,
        imageCmp: ImageComponent,
    ): Vector2 {
        val regionName =
            background.properties.get("image", String::class.java)
                ?: error("Missing 'atlasRegion' property for animated background")

        val frames = animatesBgAtlas.findRegions(regionName)
        require(frames.size > 0) { "No frames found in atlas for '$regionName'" }

        val frameDuration = 64f / 1000f

        val drawables = Array<TextureRegionDrawable>(frames.size)

        for (frame in frames) drawables.add(TextureRegionDrawable(frame))
        val animation =
            Animation(frameDuration, drawables).apply {
                playMode = Animation.PlayMode.LOOP
            }

        entity +=
            AnimationComponent().apply {
                this.animation = animation
            }

        imageCmp.image = Image(animation.keyFrames.first())

        frames.first()?.texture?.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.ClampToEdge)

        return vec2(frames.first().originalWidth.toFloat(), frames.first().originalHeight.toFloat())
    }
}
