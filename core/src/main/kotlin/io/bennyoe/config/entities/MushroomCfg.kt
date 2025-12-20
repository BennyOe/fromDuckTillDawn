package io.bennyoe.config.entities

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.physics.box2d.BodyDef
import io.bennyoe.assets.SoundAssets
import io.bennyoe.components.Attack
import io.bennyoe.components.AttackType
import io.bennyoe.components.ai.SensorDef
import io.bennyoe.components.animation.AnimationModel
import io.bennyoe.components.animation.MushroomAnimation
import io.bennyoe.config.CharacterType
import io.bennyoe.config.EntityCategory
import io.bennyoe.config.GameConstants.CHASE_DETECTION_RADIUS
import io.bennyoe.config.GameConstants.CHASE_SPEED
import io.bennyoe.config.GameConstants.NORMAL_DETECTION_RADIUS
import io.bennyoe.config.SpawnCfgFactory
import io.bennyoe.systems.audio.SoundProfile
import io.bennyoe.systems.audio.SoundType
import io.bennyoe.systems.render.ZIndex
import io.bennyoe.utility.FloorType
import io.bennyoe.utility.SensorType
import ktx.math.vec2
import kotlin.experimental.or

object MushroomCfg {
    val config =
        SpawnCfgFactory(
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
            maxSightRadius = 7f,
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
            aiTreePath = "ai/mushroom.tree",
            scaleSpeed = 0.5f,
            keepCorpse = true,
            scaleImage = vec2(0.6f, 0.8f),
            scalePhysic = vec2(0.3f, 0.5f),
            offsetPhysic = vec2(0f, -0.8f),
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
            basicSensorList =
                listOf(
                    // Wall Sensor: Checks for walls in front of the entity
                    SensorDef(
                        bodyAnchorPoint = vec2(1f, -0.8f),
                        rayLengthOffset = vec2(0.5f, 0f),
                        type = SensorType.WALL_SENSOR,
                        name = "mushroom_wall",
                        color = Color.BLUE,
                        hitFilter = {
                            it.entityCategory == EntityCategory.GROUND ||
                                it.entityCategory == EntityCategory.WORLD_BOUNDARY
                        },
                    ),
                    // Wall Height Sensor: Checks if the wall is jumpable (empty space above wall)
                    SensorDef(
                        bodyAnchorPoint = vec2(1f, 1.5f),
                        rayLengthOffset = vec2(0.5f, 0f),
                        type = SensorType.WALL_HEIGHT_SENSOR,
                        name = "mushroom_wall_height",
                        color = Color.BLUE,
                        hitFilter = { it.entityCategory == EntityCategory.GROUND },
                    ),
                    // Ground Sensor: Detects the ground (or lack thereof/ledge) in front
                    SensorDef(
                        bodyAnchorPoint = vec2(1f, -1f),
                        rayLengthOffset = vec2(0f, -1.4f),
                        type = SensorType.GROUND_DETECT_SENSOR,
                        name = "mushroom_ground",
                        color = Color.GREEN,
                    ),
                    // Jump Sensor: Checks for a landing spot across a gap
                    SensorDef(
                        bodyAnchorPoint = vec2(3.2f, -1f),
                        rayLengthOffset = vec2(0f, -1.4f),
                        type = SensorType.JUMP_SENSOR,
                        name = "mushroom_jump",
                        color = Color.GREEN,
                    ),
                    // Attack Sensor: Detects the player in melee range
                    SensorDef(
                        bodyAnchorPoint = vec2(1f, -0.7f),
                        rayLengthOffset = vec2(1f, 0f),
                        type = SensorType.ATTACK_SENSOR,
                        name = "mushroom_attack",
                        color = Color.ORANGE,
                        hitFilter = { it.entityCategory == EntityCategory.PLAYER },
                    ),
                ),
        )
}
