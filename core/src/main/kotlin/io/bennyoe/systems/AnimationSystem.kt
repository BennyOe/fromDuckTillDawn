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
import io.bennyoe.components.AnimationModel
import io.bennyoe.components.AnimationType
import io.bennyoe.components.ImageComponent
import ktx.app.gdxError
import ktx.collections.map
import ktx.log.logger

class AnimationSystem(
    dawnAtlas: TextureAtlas = inject("dawnAtlas"),
    dawnNormalAtlas: TextureAtlas = inject("dawnNormalAtlas"),
    mushroomAtlas: TextureAtlas = inject("mushroomAtlas"),
) : IteratingSystem(family { all(AnimationComponent, ImageComponent) }),
    PausableSystem {
    private val cachedAnimations = mutableMapOf<String, Animation<TextureRegionDrawable>>()

    private val atlasMap: Map<AnimationModel, TextureAtlas> =
        mapOf(
            AnimationModel.PLAYER_DAWN to dawnAtlas,
            AnimationModel.ENEMY_MUSHROOM to mushroomAtlas,
        )

    private val normalAtlasMap: Map<AnimationModel, TextureAtlas> =
        mapOf(
            AnimationModel.PLAYER_DAWN to dawnNormalAtlas,
        )

    override fun onTickEntity(entity: Entity) {
        val aniCmp = entity[AnimationComponent]
        val imageCmp = entity[ImageComponent]

        if (aniCmp.nextAnimationType != AnimationType.NONE) {
            applyNextAnimation(aniCmp)
        }

        aniCmp.stateTime += deltaTime
        aniCmp.animation.playMode = aniCmp.mode
        val currentFrame = aniCmp.animation.getKeyFrame(aniCmp.stateTime)
        imageCmp.image.drawable = currentFrame
    }

    private fun applyNextAnimation(aniCmp: AnimationComponent): TextureRegionDrawable {
        val currentAtlas =
            atlasMap[aniCmp.animationModel]
                ?: gdxError("No texture atlas for model '${aniCmp.animationModel}' in EntitySpawnSystem found.")

        val aniKeyPath = aniCmp.nextAnimationType.atlasKey + aniCmp.nextAnimationVariant.atlasKey

        aniCmp.animation =
            setTexturesToAnimation(
                currentAtlas,
                aniKeyPath,
                aniCmp.nextAnimationType.speed,
                aniCmp.nextAnimationType.playMode,
            )
        aniCmp.clearAnimation()
        aniCmp.stateTime = 0f

        val firstFrame =
            if (aniCmp.isReversed) {
                aniCmp.animation.keyFrames.size
                    .toFloat()
            } else {
                0f
            }
        return aniCmp.animation.getKeyFrame(firstFrame)
    }

    private fun setTexturesToAnimation(
        atlas: TextureAtlas,
        aniKeyPath: String,
        speed: Float,
        playMode: PlayMode,
    ): Animation<TextureRegionDrawable> =
        cachedAnimations.getOrPut("${atlas.hashCode()}_$aniKeyPath") {
            val regions = atlas.findRegions(aniKeyPath)
            if (regions.isEmpty) {
                gdxError("No regions for path '$aniKeyPath' found in atlas.")
            }
            Animation(speed, regions.map { TextureRegionDrawable(it) }).apply {
                this.playMode = playMode
            }
        }

    companion object {
        private val logger = logger<AnimationSystem>()
    }
}
