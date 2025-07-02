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
import io.bennyoe.components.NormalMappedRenderingComponent
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
        val normalMappedRenderingComponent = entity.getOrNull(NormalMappedRenderingComponent)

        // 1. If a new animation is requested, set it up.
        if (aniCmp.nextAnimationType != AnimationType.NONE) {
            applyNextAnimation(aniCmp)
        }

        // 2. Let the animation progress and get the current diffuse frame.
        aniCmp.stateTime += deltaTime
        aniCmp.animation.playMode = aniCmp.mode
        val currentFrameDrawable = aniCmp.animation.getKeyFrame(aniCmp.stateTime)
        imageCmp.image.drawable = currentFrameDrawable

        // 3. Find the matching normal frame for the CURRENT diffuse frame.
        if (normalMappedRenderingComponent != null) {
            val currentDiffuseRegion = currentFrameDrawable.region as TextureAtlas.AtlasRegion
            normalMappedRenderingComponent.diffuse = currentDiffuseRegion

            // Check if the current animation type should use a normal map.
            if (aniCmp.currentAnimationType.normalMap) {
                val normalAtlas = normalAtlasMap[aniCmp.animationModel]
                if (normalAtlas != null) {
                    val normalRegion = normalAtlas.findRegion(currentDiffuseRegion.name, currentDiffuseRegion.index)

                    if (normalRegion != null) {
                        normalMappedRenderingComponent.normal = normalRegion
                    } else {
                        logger.error {
                            "Normal map region '${currentDiffuseRegion.name}' with index ${currentDiffuseRegion.index} not found in normal atlas!"
                        }
                        normalMappedRenderingComponent.normal = null
                    }
                } else {
                    normalMappedRenderingComponent.normal = null
                }
            } else {
                // There is no normal map for this animation type
                normalMappedRenderingComponent.normal = null
            }
        }
    }

    private fun applyNextAnimation(aniCmp: AnimationComponent) {
        val currentAtlas =
            atlasMap[aniCmp.animationModel]
                ?: gdxError("No texture atlas found for model '${aniCmp.animationModel}'.")

        val aniKeyPath = aniCmp.nextAnimationType.atlasKey + aniCmp.nextAnimationVariant.atlasKey

        // Save the new animation frame as the current
        aniCmp.currentAnimationType = aniCmp.nextAnimationType

        aniCmp.animation =
            setTexturesToAnimation(
                currentAtlas,
                aniKeyPath,
                aniCmp.nextAnimationType.speed,
                aniCmp.nextAnimationType.playMode,
            )

        // Reset the request for a new animation.
        aniCmp.clearAnimation()
        aniCmp.stateTime = 0f
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
                gdxError("No regions found for path '$aniKeyPath' in the atlas.")
            }
            Animation(speed, regions.map { TextureRegionDrawable(it) }).apply {
                this.playMode = playMode
            }
        }

    companion object {
        private val logger = logger<AnimationSystem>()
    }
}
