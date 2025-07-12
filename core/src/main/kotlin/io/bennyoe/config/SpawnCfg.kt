package io.bennyoe.config

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.BodyDef
import io.bennyoe.components.AnimationModel
import io.bennyoe.components.AnimationType
import io.bennyoe.components.AnimationVariant
import io.bennyoe.config.GameConstants.CHASE_DETECTION_RADIUS
import io.bennyoe.config.GameConstants.CHASE_SPEED
import io.bennyoe.config.GameConstants.NORMAL_DETECTION_RADIUS
import ktx.app.gdxError
import ktx.math.vec2
import kotlin.experimental.or

data class SpawnCfg(
    val animationModel: AnimationModel = AnimationModel.NONE,
    val animationType: AnimationType = AnimationType.NONE,
    val animationVariant: AnimationVariant = AnimationVariant.NONE,
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
                                    EntityCategory.SENSOR.bit
                            ),
                            animationModel = AnimationModel.PLAYER_DAWN,
                            animationType = AnimationType.IDLE,
                            animationVariant = AnimationVariant.FIRST,
                            bodyType = BodyDef.BodyType.DynamicBody,
                            canAttack = true,
                            attackDelay = 0.1f,
                            scaleImage = vec2(4f, 2f),
                            scalePhysic = vec2(0.2f, 0.5f),
                            keepCorpse = true,
                            removeDelay = 1f,
                            zIndex = 10,
                        )

                    "enemy" ->
                        SpawnCfg(
                            entityCategory = EntityCategory.ENEMY,
                            physicMaskCategory = (
                                EntityCategory.GROUND.bit or
                                    EntityCategory.WORLD_BOUNDARY.bit
                            ),
                            animationModel = AnimationModel.ENEMY_MUSHROOM,
                            animationType = AnimationType.IDLE,
                            animationVariant = AnimationVariant.FIRST,
                            bodyType = BodyDef.BodyType.DynamicBody,
                            canAttack = true,
                            attackDelay = 0.5f,
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
                            zIndex = 5,
                        )

                    else -> gdxError("There is no spawn configuration for entity-type $type")
                }
            }
    }
}
