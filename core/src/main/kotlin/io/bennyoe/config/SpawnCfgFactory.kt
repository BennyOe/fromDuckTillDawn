package io.bennyoe.config

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.BodyDef
import io.bennyoe.components.Attack
import io.bennyoe.components.AttackType
import io.bennyoe.components.ai.SensorDef
import io.bennyoe.components.animation.AnimationKey
import io.bennyoe.components.animation.AnimationModel
import io.bennyoe.components.animation.NoAnimationKey
import io.bennyoe.config.entities.MinotaurCfg
import io.bennyoe.config.entities.MushroomCfg
import io.bennyoe.config.entities.PlayerCfg
import io.bennyoe.systems.audio.SoundProfile
import io.bennyoe.systems.audio.SoundType
import io.bennyoe.systems.render.ZIndex
import ktx.math.vec2

data class SpawnCfgFactory(
    val animationModel: AnimationModel = AnimationModel.NONE,
    val animationType: AnimationKey = NoAnimationKey,
    val animationSpeed: Float = 1f,
    val bodyType: BodyDef.BodyType = BodyDef.BodyType.StaticBody,
    val entityCategory: EntityCategory = EntityCategory.GROUND,
    val physicMaskCategory: Short = 0x0000,
    val canAttack: Boolean = false,
    val attackMap: Map<AttackType, Attack> = mapOf(),
    val maxSightRadius: Float = 1f,
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
    val basicSensorList: List<SensorDef> = emptyList(),
    val sightSensorDefinition: SensorDef? = null,
) {
    companion object {
        val cachedSpawnCfgsFactory = mutableMapOf<CharacterType, SpawnCfgFactory>()

        fun createSpawnCfg(characterType: CharacterType): SpawnCfgFactory =
            cachedSpawnCfgsFactory.getOrPut(characterType) {
                return when (characterType) {
                    CharacterType.PLAYER -> PlayerCfg.config

                    CharacterType.MUSHROOM -> MushroomCfg.config

                    CharacterType.MINOTAUR -> MinotaurCfg.config
                }
            }
    }
}
