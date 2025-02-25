package io.bennyoe.systems

import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import io.bennyoe.components.AnimationComponent
import io.bennyoe.components.AnimationComponent.Companion.NO_ANIMATION
import io.bennyoe.components.ImageComponent
import ktx.app.gdxError
import ktx.collections.map

class AnimationSystem(
    private val textureAtlas: TextureAtlas,
) : IteratingSystem(family { all(AnimationComponent, ImageComponent) }) {
    private val cachedAnimations = mutableMapOf<String, Animation<TextureRegionDrawable>>()

    override fun onTickEntity(entity: Entity) {

        val aniCmp = entity[AnimationComponent]
        if (aniCmp.nextAnimation != NO_ANIMATION){
            aniCmp.run {
                animation = animation(aniCmp.nextAnimation)
                stateTime = 0f
                nextAnimation = NO_ANIMATION
            }
        } else {
            aniCmp.run {
                aniCmp.stateTime += deltaTime
                aniCmp.animation.playMode = aniCmp.playMode
                aniCmp.animation.getKeyFrame(aniCmp.stateTime)
            }
        }

        aniCmp.animation.playMode = aniCmp.playMode
        entity[ImageComponent].image.drawable = aniCmp.animation.getKeyFrame(aniCmp.stateTime)
    }

    private fun animation(aniKeyPath: String): Animation<TextureRegionDrawable> {
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
