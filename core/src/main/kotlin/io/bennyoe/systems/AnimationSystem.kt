package io.bennyoe.systems

import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import com.github.quillraven.fleks.World.Companion.inject
import io.bennyoe.components.AnimationComponent
import io.bennyoe.components.AnimationType
import io.bennyoe.components.ImageComponent
import ktx.app.gdxError
import ktx.collections.map

class AnimationSystem(
    private val textureAtlas: TextureAtlas = inject(),
) : IteratingSystem(family { all(AnimationComponent, ImageComponent) }) {
    private val cachedAnimations = mutableMapOf<String, Animation<TextureRegionDrawable>>()

    override fun onTickEntity(entity: Entity) {

        val aniCmp = entity[AnimationComponent]
        with(entity[ImageComponent]) {
            flipImage = aniCmp.flipImage
            image.drawable = if (aniCmp.nextAnimation != AnimationType.NONE) {
                aniCmp.run {
                    animation = setTexturesToAnimation(aniCmp.nextAnimation.atlasKey)
                    clearAnimation()
                    stateTime = 0f
                    animation.playMode = playMode
                    animation.getKeyFrame(0f)
                }
            } else {
                aniCmp.run {
                    stateTime += deltaTime
                    animation.playMode = playMode
                    animation.getKeyFrame(aniCmp.stateTime)
                }
            }
        }
    }

    private fun setTexturesToAnimation(aniKeyPath: String): Animation<TextureRegionDrawable> {
        return cachedAnimations.getOrPut(aniKeyPath) {
            val regions = textureAtlas.findRegions(aniKeyPath)
            if (regions.isEmpty) {
                gdxError("There are no regions for $aniKeyPath")
            }
            Animation(DEFAULT_FRAME_DURATION, regions.map { TextureRegionDrawable(it) })
        }
    }

    companion object {
        const val DEFAULT_FRAME_DURATION = 1 / 8f
    }
}
