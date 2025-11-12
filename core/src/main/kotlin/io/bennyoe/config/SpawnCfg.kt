package io.bennyoe.config

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.BodyDef
import io.bennyoe.assets.SoundAssets
import io.bennyoe.components.Attack
import io.bennyoe.components.AttackType
import io.bennyoe.components.animation.AnimationKey
import io.bennyoe.components.animation.AnimationModel
import io.bennyoe.components.animation.MinotaurAnimation
import io.bennyoe.components.animation.MushroomAnimation
import io.bennyoe.components.animation.NoAnimationKey
import io.bennyoe.components.animation.PlayerAnimation
import io.bennyoe.config.GameConstants.CHASE_DETECTION_RADIUS
import io.bennyoe.config.GameConstants.CHASE_SPEED
import io.bennyoe.config.GameConstants.JUMP_MAX_HEIGHT
import io.bennyoe.config.GameConstants.NORMAL_DETECTION_RADIUS
import io.bennyoe.systems.audio.SoundProfile
import io.bennyoe.systems.audio.SoundType
import io.bennyoe.systems.render.ZIndex
import io.bennyoe.utility.FloorType
import ktx.math.vec2
import kotlin.experimental.or

data class SpawnCfg(
    val animationModel: AnimationModel = AnimationModel.NONE,
    val animationType: AnimationKey = NoAnimationKey,
    val animationSpeed: Float = 1f,
    val bodyType: BodyDef.BodyType = BodyDef.BodyType.StaticBody,
    val entityCategory: EntityCategory = EntityCategory.GROUND,
    val physicMaskCategory: Short = 0x0000,
    val canAttack: Boolean = false,
    val attackMap: Map<AttackType, Attack> = mapOf(),
    val characterType: CharacterType = CharacterType.PLAYER,
    val jumpHeight: Float = 5f,
    val scalePhysic: Vector2 = vec2(1f, 1f),
    val offsetPhysic: Vector2 = vec2(0f, 0f),
    val scaleImage: Vector2 = vec2(1f, 1f),
    val zIndex: Int = ZIndex.MIN.value,
    val scaleSpeed: Float = 1f,
    val aiTreePath: String = "",
    val keepCorpse: Boolean = false,
    val removeDelay: Float = 0f,
    val nearbyEnemiesDefaultSensorRadius: Float = 4f,
    val nearbyEnemiesExtendedSensorRadius: Float = 7f,
    val nearbyEnemiesSensorOffset: Vector2 = vec2(0f, 0f),
    val chaseSpeed: Float = 0f,
    // the soundTrigger is a map of <AnimationType, <FrameWhereSoundIsTriggered, SoundType>>
    val soundTrigger: Map<AnimationKey, Map<Int, SoundType>> = emptyMap(),
    val soundProfile: SoundProfile = SoundProfile(),
) {
    companion object {
        val cachedSpawnCfgs = mutableMapOf<CharacterType, SpawnCfg>()

        fun createSpawnCfg(characterType: CharacterType): SpawnCfg =
            cachedSpawnCfgs.getOrPut(characterType) {
                when (characterType) {
                    CharacterType.PLAYER ->
                        SpawnCfg(
                            entityCategory = EntityCategory.PLAYER,
                            physicMaskCategory = (
                                EntityCategory.GROUND.bit or
                                    EntityCategory.WORLD_BOUNDARY.bit or
                                    EntityCategory.SENSOR.bit or
                                    EntityCategory.ENEMY.bit or
                                    EntityCategory.WATER.bit or
                                    EntityCategory.LANTERN.bit
                            ),
                            animationModel = AnimationModel.PLAYER_DAWN,
                            animationType = PlayerAnimation.IDLE,
                            bodyType = BodyDef.BodyType.DynamicBody,
                            characterType = CharacterType.PLAYER,
                            canAttack = true,
                            attackMap =
                                mapOf(
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
                            scaleImage = vec2(4f, 2f),
                            scalePhysic = vec2(0.2f, 0.5f),
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

                    CharacterType.MUSHROOM ->
                        SpawnCfg(
                            entityCategory = EntityCategory.ENEMY,
                            physicMaskCategory = (
                                EntityCategory.GROUND.bit or
                                    EntityCategory.WORLD_BOUNDARY.bit or
                                    EntityCategory.PLAYER.bit or
                                    EntityCategory.WATER.bit or
                                    EntityCategory.SENSOR.bit
                            ),
                            animationModel = AnimationModel.ENEMY_MUSHROOM,
                            animationType = MushroomAnimation.IDLE,
                            bodyType = BodyDef.BodyType.DynamicBody,
                            characterType = CharacterType.MUSHROOM,
                            canAttack = true,
                            attackMap =
                                mapOf(
                                    AttackType.HEADNUT to
                                        Attack(
                                            AttackType.HEADNUT,
                                            CharacterType.MUSHROOM,
                                            5f,
                                            5f,
                                            1f,
                                            0.3f,
                                        ),
                                ),
                            scaleImage = vec2(3f, 3f),
                            scalePhysic = vec2(0.2f, 0.4f),
                            offsetPhysic = vec2(0f, -0.7f),
                            aiTreePath = "ai/mushroom.tree",
                            scaleSpeed = 0.5f,
                            keepCorpse = true,
                            removeDelay = 2f,
                            nearbyEnemiesDefaultSensorRadius = NORMAL_DETECTION_RADIUS,
                            nearbyEnemiesExtendedSensorRadius = CHASE_DETECTION_RADIUS,
                            nearbyEnemiesSensorOffset = vec2(0f, 0f),
                            chaseSpeed = CHASE_SPEED,
                            zIndex = ZIndex.ENEMY_OFFSET.value,
                            soundTrigger =
                                mapOf(
                                    MushroomAnimation.WALK to
                                        mapOf(
                                            0 to SoundType.MUSHROOM_FOOTSTEPS,
                                            2 to SoundType.MUSHROOM_FOOTSTEPS,
                                            4 to SoundType.MUSHROOM_FOOTSTEPS,
                                            6 to SoundType.MUSHROOM_FOOTSTEPS,
                                        ),
                                    MushroomAnimation.HIT to
                                        mapOf(
                                            1 to SoundType.MUSHROOM_HIT,
                                        ),
                                    MushroomAnimation.ATTACK_1 to
                                        mapOf(
                                            1 to SoundType.MUSHROOM_ATTACK,
                                        ),
                                    MushroomAnimation.DYING to
                                        mapOf(
                                            1 to SoundType.MUSHROOM_DEATH,
                                        ),
                                    MushroomAnimation.JUMP to
                                        mapOf(
                                            1 to SoundType.MUSHROOM_JUMP,
                                        ),
                                ),
                            soundProfile =
                                SoundProfile(
                                    simpleSounds =
                                        mapOf(
                                            SoundType.MUSHROOM_HIT to listOf(SoundAssets.MUSHROOM_HIT_SOUND),
                                            SoundType.MUSHROOM_ATTACK to listOf(SoundAssets.MUSHROOM_ATTACK_SOUND),
                                            SoundType.MUSHROOM_DEATH to listOf(SoundAssets.MUSHROOM_DEATH_SOUND),
                                            SoundType.MUSHROOM_JUMP to listOf(SoundAssets.MUSHROOM_JUMP_SOUND),
                                        ),
                                    footstepsSounds =
                                        mapOf(
                                            FloorType.STONE to listOf(SoundAssets.MUSHROOM_FOOTSTEPS_STONE),
                                            FloorType.GRASS to listOf(SoundAssets.MUSHROOM_FOOTSTEPS_GRASS),
                                        ),
                                ),
                        )

                    CharacterType.MINOTAUR ->
                        SpawnCfg(
                            entityCategory = EntityCategory.ENEMY,
                            physicMaskCategory = (
                                EntityCategory.GROUND.bit or
                                    EntityCategory.WORLD_BOUNDARY.bit or
                                    EntityCategory.PLAYER.bit or
                                    EntityCategory.WATER.bit or
                                    EntityCategory.SENSOR.bit
                            ),
                            animationModel = AnimationModel.ENEMY_MINOTAUR,
                            animationType = MinotaurAnimation.IDLE,
                            animationSpeed = 1.4f,
                            bodyType = BodyDef.BodyType.DynamicBody,
                            characterType = CharacterType.MINOTAUR,
                            canAttack = true,
                            attackMap =
                                mapOf(
                                    // TODO needs to be changed to AttackType.AXE as soon as the Minotaur gets his own FSM and AI
                                    AttackType.HEADNUT to
                                        Attack(
                                            AttackType.HEADNUT,
                                            CharacterType.MINOTAUR,
                                            20f,
                                            25f,
                                            4f,
                                            0.3f,
                                            true,
                                            .3f,
                                        ),
                                ),
                            jumpHeight = 10f,
                            scaleImage = vec2(28f, 17f),
                            scalePhysic = vec2(1f, 2.5f),
                            offsetPhysic = vec2(0f, -1.83f),
                            aiTreePath = "ai/minotaur.tree",
                            scaleSpeed = 0.5f,
                            keepCorpse = false,
                            removeDelay = 2f,
                            nearbyEnemiesDefaultSensorRadius = NORMAL_DETECTION_RADIUS * 2f,
                            nearbyEnemiesExtendedSensorRadius = CHASE_DETECTION_RADIUS * 2f,
                            nearbyEnemiesSensorOffset = vec2(0f, 0f),
                            chaseSpeed = CHASE_SPEED,
                            zIndex = ZIndex.ENEMY_OFFSET.value,
                            soundTrigger =
                                mapOf(
                                    MinotaurAnimation.WALK to
                                        mapOf(
                                            0 to SoundType.MUSHROOM_FOOTSTEPS,
                                            2 to SoundType.MUSHROOM_FOOTSTEPS,
                                            4 to SoundType.MUSHROOM_FOOTSTEPS,
                                            6 to SoundType.MUSHROOM_FOOTSTEPS,
                                        ),
                                    MinotaurAnimation.HIT to
                                        mapOf(
                                            1 to SoundType.MUSHROOM_HIT,
                                        ),
                                    MinotaurAnimation.DYING to
                                        mapOf(
                                            1 to SoundType.MUSHROOM_DEATH,
                                        ),
                                ),
                            soundProfile =
                                SoundProfile(
                                    simpleSounds =
                                        mapOf(
                                            SoundType.MUSHROOM_HIT to listOf(SoundAssets.MUSHROOM_HIT_SOUND),
                                            SoundType.MUSHROOM_ATTACK to listOf(SoundAssets.MUSHROOM_ATTACK_SOUND),
                                            SoundType.MUSHROOM_DEATH to listOf(SoundAssets.MUSHROOM_DEATH_SOUND),
                                            SoundType.MUSHROOM_JUMP to listOf(SoundAssets.MUSHROOM_JUMP_SOUND),
                                        ),
                                    footstepsSounds =
                                        mapOf(
                                            FloorType.STONE to listOf(SoundAssets.MUSHROOM_FOOTSTEPS_STONE),
                                            FloorType.GRASS to listOf(SoundAssets.MUSHROOM_FOOTSTEPS_GRASS),
                                        ),
                                ),
                        )
                }
            }
    }
}
