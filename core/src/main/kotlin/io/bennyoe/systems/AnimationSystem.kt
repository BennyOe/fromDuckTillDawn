package io.bennyoe.systems

import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.Animation.PlayMode
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import com.github.quillraven.fleks.World.Companion.inject
import io.bennyoe.assets.TextureAtlases
import io.bennyoe.components.DisabledComponent
import io.bennyoe.components.ImageComponent
import io.bennyoe.components.PhysicComponent
import io.bennyoe.components.ShaderRenderingComponent
import io.bennyoe.components.TransformComponent
import io.bennyoe.components.animation.AnimationComponent
import io.bennyoe.components.animation.AnimationModel
import io.bennyoe.components.animation.NoAnimationKey
import io.bennyoe.event.PlaySoundEvent
import io.bennyoe.event.fire
import io.bennyoe.systems.audio.SoundType
import ktx.app.gdxError
import ktx.collections.map
import ktx.log.logger

/**
 * System responsible for updating entity animations in the ECS.
 *
 * Handles both tile-based and model-based animations, supporting diffuse, normal, and specular maps for advanced rendering.
 *
 * **Flow:**
 * - For each entity with `AnimationComponent` and `ImageComponent`:
 *   - If the animation model is `NONE`, updates the animation frame for simple tile animations.
 *   - Otherwise, manages model-based animations:
 *     - Applies a new animation if requested.
 *     - Advances the animation state and updates the displayed frame.
 *     - If the entity has a `ShaderRenderingComponent`, sets the corresponding normal and specular map regions for the current frame.
 * - Caches animations for performance.
 *
 * Integrates with Fleks ECS, uses injected texture atlases, and supports animation switching, play modes, and speed.
 */
class AnimationSystem(
    dawnAtlases: TextureAtlases = inject("dawnAtlases"),
    mushroomAtlases: TextureAtlases = inject("mushroomAtlases"),
    minotaurAtlases: TextureAtlases = inject("minotaurAtlases"),
    crowAtlases: TextureAtlases = inject("crowAtlases"),
    val stage: Stage = inject("stage"),
) : IteratingSystem(family { all(AnimationComponent, ImageComponent, TransformComponent) }),
    PausableSystem {
    private val cachedAnimations = mutableMapOf<String, Animation<TextureRegionDrawable>>()

    private val atlasMap: Map<AnimationModel, TextureAtlas> =
        mapOf(
            AnimationModel.PLAYER_DAWN to dawnAtlases.diffuseAtlas,
            AnimationModel.ENEMY_MUSHROOM to mushroomAtlases.diffuseAtlas,
            AnimationModel.ENEMY_MINOTAUR to minotaurAtlases.diffuseAtlas,
            AnimationModel.CROW to crowAtlases.diffuseAtlas,
        )

    private val normalAtlasMap: Map<AnimationModel, TextureAtlas> =
        mapOf(
            AnimationModel.PLAYER_DAWN to dawnAtlases.normalAtlas!!,
            AnimationModel.ENEMY_MUSHROOM to mushroomAtlases.normalAtlas!!,
            AnimationModel.CROW to crowAtlases.normalAtlas!!,
        )

    private val specularAtlasMap: Map<AnimationModel, TextureAtlas> =
        mapOf(
            AnimationModel.PLAYER_DAWN to dawnAtlases.specularAtlas!!,
            AnimationModel.ENEMY_MUSHROOM to mushroomAtlases.specularAtlas!!,
            AnimationModel.CROW to crowAtlases.specularAtlas!!,
        )

    override fun onTickEntity(entity: Entity) {
        val aniCmp = entity[AnimationComponent]
        val imageCmp = entity[ImageComponent]
        val transformCmp = entity[TransformComponent]
        val physicCmp = entity.getOrNull(PhysicComponent)

        if (entity has DisabledComponent) return

        if (aniCmp.animationModel == AnimationModel.NONE) {
            // Simplified logic for tile animations
            aniCmp.stateTime += deltaTime
            val currentFrameDrawable = aniCmp.animation.getKeyFrame(aniCmp.stateTime)
            imageCmp.image.drawable = currentFrameDrawable
        } else {
            // Logic for model-based animations
            val shaderRenderingComponent = entity.getOrNull(ShaderRenderingComponent)

            // 1. If a new animation is requested, set it up.
            if (aniCmp.nextAnimationType != NoAnimationKey) {
                applyNextAnimation(aniCmp)
            }

            // 2. Let the animation progress and get the current diffuse frame.
            aniCmp.stateTime += deltaTime
            aniCmp.animation.playMode = aniCmp.mode

            // 3. Sound triggering
            val currentFrameIndex = aniCmp.animation.getKeyFrameIndex(aniCmp.stateTime)
            if (currentFrameIndex != aniCmp.previousFrameIndex) {
                // A frame change happened
                aniCmp.animationSoundTriggers[aniCmp.currentAnimationType]?.get(currentFrameIndex)?.let { soundType: SoundType ->
                    stage.fire(
                        PlaySoundEvent(
                            entity,
                            soundType = soundType,
                            volume = 1f,
                            position = if (soundType.positional) transformCmp.position else null,
                            floorType = physicCmp?.floorType,
                        ),
                    )
                }
                aniCmp.previousFrameIndex = currentFrameIndex
            }

            val currentFrameDrawable = aniCmp.animation.getKeyFrame(aniCmp.stateTime)
            imageCmp.image.drawable = currentFrameDrawable

            // 4. Find the matching normal frame for the CURRENT diffuse frame.
            if (shaderRenderingComponent != null) {
                val currentDiffuseRegion = currentFrameDrawable.region as TextureAtlas.AtlasRegion
                shaderRenderingComponent.diffuse = currentDiffuseRegion

                // Check if the current animation type should use a normal map.
                setNormalMap(aniCmp, currentDiffuseRegion, shaderRenderingComponent)

                // Check if the current animation type should use a specular map.
                setSpecularMap(aniCmp, currentDiffuseRegion, shaderRenderingComponent)
            }
        }
    }

    private fun setNormalMap(
        aniCmp: AnimationComponent,
        currentDiffuseRegion: TextureAtlas.AtlasRegion,
        shaderRenderingComponent: ShaderRenderingComponent,
    ) {
        if (aniCmp.currentAnimationType.normalMap) {
            val normalAtlas = normalAtlasMap[aniCmp.animationModel]
            if (normalAtlas != null) {
                val normalRegion = normalAtlas.findRegion(currentDiffuseRegion.name, currentDiffuseRegion.index)

                if (normalRegion != null) {
                    shaderRenderingComponent.normal = normalRegion
                } else {
//                    logger.error {
//                        "Normal map region '${currentDiffuseRegion.name}' " +
//                            "with index ${currentDiffuseRegion.index} not found in normal atlas!"
//                    }
                    shaderRenderingComponent.normal = null
                }
            } else {
                shaderRenderingComponent.normal = null
            }
        } else {
            // There is no normal map for this animation type
            shaderRenderingComponent.normal = null
        }
    }

    private fun setSpecularMap(
        aniCmp: AnimationComponent,
        currentDiffuseRegion: TextureAtlas.AtlasRegion,
        shaderRenderingComponent: ShaderRenderingComponent,
    ) {
        if (aniCmp.currentAnimationType.specularMap) {
            val specularAtlas = specularAtlasMap[aniCmp.animationModel]
            if (specularAtlas != null) {
                val specularRegion = specularAtlas.findRegion(currentDiffuseRegion.name, currentDiffuseRegion.index)

                if (specularRegion != null) {
                    shaderRenderingComponent.specular = specularRegion
                } else {
//                    logger.error {
//                        "Specular map region '${currentDiffuseRegion.name}' " +
//                            "with index ${currentDiffuseRegion.index} not found in specular atlas!"
//                    }
                    shaderRenderingComponent.specular = null
                }
            } else {
                shaderRenderingComponent.specular = null
            }
        } else {
            // There is no specular map for this animation type
            shaderRenderingComponent.specular = null
        }
    }

    private fun applyNextAnimation(aniCmp: AnimationComponent) {
        val currentAtlas =
            atlasMap[aniCmp.animationModel]
                ?: gdxError("No texture atlas found for model '${aniCmp.animationModel}'.")

        val aniKeyPath = aniCmp.nextAnimationType.atlasKey

        // Save the new animation frame as the current
        aniCmp.currentAnimationType = aniCmp.nextAnimationType

        aniCmp.animation =
            setTexturesToAnimation(
                currentAtlas,
                aniKeyPath,
                aniCmp.nextAnimationType.speed / aniCmp.speedMultiplier,
                aniCmp.nextAnimationType.playMode,
            )

        // Reset previousFrameIndex when a new animation is set
        aniCmp.previousFrameIndex = -1

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

/*
TODO divided animations
1. create spritesheet with half images (other half transparent)
2. decide which state has which top and bottom animation
3. separate animations code wise into the half animations
4. give every state a top and a bottom animation
5. animationSystem must handle 2 animations for characters at the same time
 */
