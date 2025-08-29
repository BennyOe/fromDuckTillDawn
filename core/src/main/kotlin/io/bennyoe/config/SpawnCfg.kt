package io.bennyoe.config

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.BodyDef
import io.bennyoe.assets.SoundAssets
import io.bennyoe.components.AnimationModel
import io.bennyoe.components.AnimationType
import io.bennyoe.config.GameConstants.CHASE_DETECTION_RADIUS
import io.bennyoe.config.GameConstants.CHASE_SPEED
import io.bennyoe.config.GameConstants.NORMAL_DETECTION_RADIUS
import io.bennyoe.systems.audio.SoundProfile
import io.bennyoe.systems.audio.SoundType
import io.bennyoe.utility.FloorType
import ktx.app.gdxError
import ktx.math.vec2
import kotlin.experimental.or

data class SpawnCfg(
    val animationModel: AnimationModel = AnimationModel.NONE,
    val animationType: AnimationType = AnimationType.NONE,
    val bodyType: BodyDef.BodyType = BodyDef.BodyType.StaticBody,
    val entityCategory: EntityCategory = EntityCategory.GROUND,
    val physicMaskCategory: Short = 0x0000,
    val canAttack: Boolean = false,
    val attackDelay: Float = 0.2f,
    val scaleAttackDamage: Float = 1f,
    val attackExtraRange: Float = 1f,
    val scalePhysic: Vector2 = vec2(1f, 1f),
    val offsetPhysic: Vector2 = vec2(0f, 0f),
    val scaleImage: Vector2 = vec2(1f, 1f),
    val zIndex: Int = 1,
    val scaleSpeed: Float = 1f,
    val aiTreePath: String = "",
    val keepCorpse: Boolean = false,
    val removeDelay: Float = 0f,
    val nearbyEnemiesDefaultSensorRadius: Float = 4f,
    val nearbyEnemiesExtendedSensorRadius: Float = 7f,
    val nearbyEnemiesSensorOffset: Vector2 = vec2(0f, 0f),
    val chaseSpeed: Float = 0f,
    val soundTrigger: Map<AnimationType, Map<Int, SoundType>> = emptyMap(),
    val soundProfile: SoundProfile = SoundProfile(),
) {
    companion object {
        val cachedSpawnCfgs = mutableMapOf<String, SpawnCfg>()

        fun createSpawnCfg(type: String): SpawnCfg =
            cachedSpawnCfgs.getOrPut(type) {
                when (type) {
                    "playerStart" ->
                        SpawnCfg(
                            entityCategory = EntityCategory.PLAYER,
                            physicMaskCategory = (
                                EntityCategory.GROUND.bit or
                                    EntityCategory.WORLD_BOUNDARY.bit or
                                    EntityCategory.SENSOR.bit or
                                    EntityCategory.ENEMY.bit
                            ),
                            animationModel = AnimationModel.PLAYER_DAWN,
                            animationType = AnimationType.IDLE,
                            bodyType = BodyDef.BodyType.DynamicBody,
                            canAttack = true,
                            attackDelay = 0.1f,
                            attackExtraRange = 1.4f,
                            scaleImage = vec2(4f, 2f),
                            scalePhysic = vec2(0.2f, 0.5f),
                            keepCorpse = true,
                            removeDelay = 1f,
                            zIndex = 20,
                            soundTrigger =
                                mapOf(
                                    AnimationType.WALK to
                                        mapOf(
                                            3 to SoundType.DAWN_FOOTSTEPS,
                                            6 to SoundType.DAWN_FOOTSTEPS,
                                        ),
                                    AnimationType.ATTACK_1 to
                                        mapOf(
                                            1 to SoundType.DAWN_ATTACK_1,
                                        ),
                                    AnimationType.ATTACK_2 to
                                        mapOf(
                                            1 to SoundType.DAWN_ATTACK_2,
                                        ),
                                    AnimationType.ATTACK_3 to
                                        mapOf(
                                            1 to SoundType.DAWN_ATTACK_3,
                                        ),
                                    AnimationType.JUMP to
                                        mapOf(
                                            1 to SoundType.DAWN_JUMP,
                                        ),
                                    AnimationType.BASH to
                                        mapOf(
                                            2 to SoundType.DAWN_BASH,
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
                                        ),
                                    // Define the player's footstep sounds for each surface
                                    footstepsSounds =
                                        mapOf(
                                            FloorType.WOOD to listOf(SoundAssets.DAWN_FOOTSTEPS_WOOD),
                                            FloorType.STONE to listOf(SoundAssets.DAWN_FOOTSTEPS_STONE),
                                            FloorType.GRASS to listOf(SoundAssets.DAWN_FOOTSTEPS_GRASS),
                                        ),
                                ),
                        )

                    "enemy" ->
                        SpawnCfg(
                            entityCategory = EntityCategory.ENEMY,
                            physicMaskCategory = (
                                EntityCategory.GROUND.bit or
                                    EntityCategory.WORLD_BOUNDARY.bit or
                                    EntityCategory.PLAYER.bit
                            ),
                            animationModel = AnimationModel.ENEMY_MUSHROOM,
                            animationType = AnimationType.IDLE,
                            bodyType = BodyDef.BodyType.DynamicBody,
                            canAttack = true,
                            attackDelay = 0.3f,
                            scaleImage = vec2(3f, 3f),
                            scalePhysic = vec2(0.2f, 0.4f),
                            offsetPhysic = vec2(0f, -0.7f),
                            aiTreePath = "ai/mushroom.tree",
                            scaleSpeed = 0.5f,
                            keepCorpse = true,
                            removeDelay = .2f,
                            nearbyEnemiesDefaultSensorRadius = NORMAL_DETECTION_RADIUS,
                            nearbyEnemiesExtendedSensorRadius = CHASE_DETECTION_RADIUS,
                            nearbyEnemiesSensorOffset = vec2(0f, 0f),
                            chaseSpeed = CHASE_SPEED,
                            zIndex = 10,
                            soundTrigger =
                                mapOf(
                                    AnimationType.WALK to
                                        mapOf(
                                            0 to SoundType.FOOTSTEPS,
                                            2 to SoundType.FOOTSTEPS,
                                            4 to SoundType.FOOTSTEPS,
                                            6 to SoundType.FOOTSTEPS,
                                        ),
                                    AnimationType.HIT to
                                        mapOf(
                                            1 to SoundType.HIT,
                                        ),
                                ),
                            soundProfile =
                                SoundProfile(
                                    simpleSounds =
                                        mapOf(
                                            SoundType.HIT to listOf(SoundAssets.MUSHROOM_HIT_SOUND),
                                        ),
                                    footstepsSounds =
                                        mapOf(
                                            FloorType.WOOD to listOf(SoundAssets.MUSHROOM_FOOTSTEPS_WOOD),
                                            FloorType.STONE to listOf(SoundAssets.MUSHROOM_FOOTSTEPS_STONE),
                                            FloorType.GRASS to listOf(SoundAssets.MUSHROOM_FOOTSTEPS_GRASS),
                                        ),
                                ),
                        )

                    else -> gdxError("There is no spawn configuration for entity-type $type")
                }
            }
    }
}
