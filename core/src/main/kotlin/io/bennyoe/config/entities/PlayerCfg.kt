package io.bennyoe.config.entities

import com.badlogic.gdx.physics.box2d.BodyDef
import io.bennyoe.assets.SoundAssets
import io.bennyoe.components.Attack
import io.bennyoe.components.AttackType
import io.bennyoe.components.animation.AnimationModel
import io.bennyoe.components.animation.PlayerAnimation
import io.bennyoe.config.CharacterType
import io.bennyoe.config.EntityCategory
import io.bennyoe.config.GameConstants.JUMP_MAX_HEIGHT
import io.bennyoe.config.SpawnCfgFactory
import io.bennyoe.systems.audio.SoundProfile
import io.bennyoe.systems.audio.SoundType
import io.bennyoe.systems.render.ZIndex
import io.bennyoe.utility.FloorType
import ktx.math.vec2
import kotlin.collections.mapOf
import kotlin.experimental.or

object PlayerCfg {
    val config =
        SpawnCfgFactory(
            entityCategory = EntityCategory.PLAYER,
            physicMaskCategory = (
                EntityCategory.GROUND.bit or
                    EntityCategory.WORLD_BOUNDARY.bit or
                    EntityCategory.SENSOR.bit or
                    EntityCategory.ENEMY.bit or
                    EntityCategory.WATER.bit or
                    EntityCategory.LANTERN.bit or
                    EntityCategory.ENEMY_PROJECTILE.bit
            ),
            animationModel = AnimationModel.PLAYER_DAWN,
            animationType = PlayerAnimation.IDLE,
            bodyType = BodyDef.BodyType.DynamicBody,
            scaleImage = vec2(0.6f, 0.6f),
            scalePhysic = vec2(0.3f, 0.9f),
            offsetPhysic = vec2(0f, -0.1f),
            characterType = CharacterType.PLAYER,
            canAttack = true,
            attackMap =
                mapOf(
                    pair =
                        AttackType.SWORD to
                            Attack(
                                AttackType.SWORD,
                                CharacterType.PLAYER,
                                5f,
                                5f,
                                1.4f,
                                0.2f,
                            ),
                ),
            jumpHeight = JUMP_MAX_HEIGHT,
            keepCorpse = true,
            removeDelay = 1f,
            zIndex = ZIndex.PLAYER_OFFSET.value,
            soundTrigger =
                mapOf(
                    PlayerAnimation.WALK to
                        mapOf(
                            3 to SoundType.DAWN_FOOTSTEPS,
                            6 to SoundType.DAWN_FOOTSTEPS,
                        ),
                    PlayerAnimation.ATTACK_1 to
                        mapOf(
                            1 to SoundType.DAWN_ATTACK_1,
                        ),
                    PlayerAnimation.ATTACK_2 to
                        mapOf(
                            1 to SoundType.DAWN_ATTACK_2,
                        ),
                    PlayerAnimation.ATTACK_3 to
                        mapOf(
                            1 to SoundType.DAWN_ATTACK_3,
                        ),
//                                    AnimationType.JUMP to
//                                        mapOf(
//                                            1 to SoundType.DAWN_JUMP,
//                                        ),
                    PlayerAnimation.BASH to
                        mapOf(
                            2 to SoundType.DAWN_BASH,
                        ),
                    PlayerAnimation.HIT to
                        mapOf(
                            2 to SoundType.DAWN_HIT,
                        ),
                    PlayerAnimation.DYING to
                        mapOf(
                            2 to SoundType.DAWN_DEATH,
                        ),
                ),
            soundProfile =
                SoundProfile(
                    simpleSounds =
                        mapOf(
                            SoundType.DAWN_ATTACK_1 to listOf(SoundAssets.DAWN_ATTACK_1_SOUND),
                            SoundType.DAWN_ATTACK_2 to listOf(SoundAssets.DAWN_ATTACK_2_SOUND),
                            SoundType.DAWN_ATTACK_3 to listOf(SoundAssets.DAWN_ATTACK_3_SOUND),
                            SoundType.DAWN_JUMP to listOf(SoundAssets.DAWN_JUMP_SOUND),
                            SoundType.DAWN_HIT to listOf(SoundAssets.DAWN_HIT_SOUND),
                            SoundType.DAWN_BASH to listOf(SoundAssets.DAWN_BASH_SOUND),
                            SoundType.DAWN_DEATH to listOf(SoundAssets.DAWN_DEATH_SOUND),
                            SoundType.DAWN_WATER_SPLASH to listOf(SoundAssets.DAWN_WATER_SPLASH_SOUND),
                        ),
                    // Define the player's footstep sounds for each surface
                    footstepsSounds =
                        mapOf(
                            FloorType.STONE to listOf(SoundAssets.DAWN_FOOTSTEPS_STONE),
                            FloorType.GRASS to listOf(SoundAssets.DAWN_FOOTSTEPS_GRASS),
                        ),
                ),
        )
}
