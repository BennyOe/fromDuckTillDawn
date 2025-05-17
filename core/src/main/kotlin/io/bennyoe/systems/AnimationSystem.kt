package io.bennyoe.systems

import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.Animation.PlayMode
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
import ktx.log.logger

class AnimationSystem(
    private val textureAtlas: TextureAtlas = inject(),
) : IteratingSystem(family { all(AnimationComponent, ImageComponent) }) {
    private val cachedAnimations = mutableMapOf<String, Animation<TextureRegionDrawable>>()

    override fun onTickEntity(entity: Entity) {
        val aniCmp = entity[AnimationComponent]

        with(entity[ImageComponent]) {
            flipImage = aniCmp.flipImage
            image.drawable =
                if (aniCmp.nextAnimationType == AnimationType.NONE) {
                    // then we are in an animation
                    aniCmp.run {
                        stateTime += deltaTime
                        aniCmp.animation.playMode = aniCmp.mode
                        animation.getKeyFrame(stateTime)
                    }
                } else {
                    // then we set a new animation
                    applyNextAnimation(aniCmp)
                }
        }
    }

    private fun applyNextAnimation(aniCmp: AnimationComponent): TextureRegionDrawable {
        aniCmp.animation =
            setTexturesToAnimation(
                aniCmp.nextAnimationModel.atlasKey + aniCmp.nextAnimationType.atlasKey + aniCmp.nextAnimationVariant.atlasKey,
                aniCmp.nextAnimationType.speed,
                aniCmp.nextAnimationType.playMode,
            )
        aniCmp.clearAnimation()
        aniCmp.stateTime = 0f
        return aniCmp.animation.getKeyFrame(0f)
    }

    private fun setTexturesToAnimation(
        aniKeyPath: String,
        speed: Float,
        playMode: PlayMode,
    ): Animation<TextureRegionDrawable> =
        cachedAnimations.getOrPut(aniKeyPath) {
            val regions = textureAtlas.findRegions(aniKeyPath)
            if (regions.isEmpty) {
                gdxError("There are no regions for $aniKeyPath")
            }
            Animation(speed, regions.map { TextureRegionDrawable(it) }).apply {
                this.playMode = playMode
            }
        }

    companion object {
        const val DEFAULT_FRAME_DURATION = 1 / 8f
        private val logger = logger<AnimationSystem>()
    }
}
