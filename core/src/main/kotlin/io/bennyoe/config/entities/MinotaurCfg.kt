package io.bennyoe.config.entities

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.physics.box2d.BodyDef
import io.bennyoe.assets.SoundAssets
import io.bennyoe.components.Attack
import io.bennyoe.components.AttackType
import io.bennyoe.components.ai.SensorDef
import io.bennyoe.components.animation.AnimationModel
import io.bennyoe.components.animation.MinotaurAnimation
import io.bennyoe.config.CharacterType
import io.bennyoe.config.EntityCategory
import io.bennyoe.config.GameConstants.CHASE_DETECTION_RADIUS
import io.bennyoe.config.GameConstants.NORMAL_DETECTION_RADIUS
import io.bennyoe.config.SpawnCfgFactory
import io.bennyoe.systems.audio.SoundProfile
import io.bennyoe.systems.audio.SoundType
import io.bennyoe.systems.render.ZIndex
import io.bennyoe.utility.FloorType
import io.bennyoe.utility.SensorType
import ktx.math.vec2
import kotlin.experimental.or

object MinotaurCfg {
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
            animationModel = AnimationModel.ENEMY_MINOTAUR,
            animationType = MinotaurAnimation.IDLE,
            animationSpeed = 1.4f,
            bodyType = BodyDef.BodyType.DynamicBody,
            characterType = CharacterType.MINOTAUR,
            canAttack = true,
            maxSightRadius = 14f,
            attackMap =
                mapOf(
                    AttackType.SHAKE to
                        Attack(
                            AttackType.SHAKE,
                            CharacterType.MINOTAUR,
                            5f,
                            5f,
                            0f,
                            0.5f,
                            false,
                            0f,
                        ),
                ),
            jumpHeight = 10f,
            scaleImage = vec2(1.5f, 1.5f),
            scalePhysic = vec2(0.25f, 0.6f),
            offsetPhysic = vec2(0f, -1.5f),
            aiTreePath = "ai/minotaur.tree",
            scaleSpeed = 0.5f,
            keepCorpse = false,
            removeDelay = 2f,
            nearbyEnemiesDefaultSensorRadius = NORMAL_DETECTION_RADIUS * 2f,
            nearbyEnemiesExtendedSensorRadius = CHASE_DETECTION_RADIUS * 2f,
            nearbyEnemiesSensorOffset = vec2(0f, 0f),
            chaseSpeed = 20f,
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
            basicSensorList =
                listOf(
                    // Wall Sensor: Detects walls earlier because the Minotaur is faster/bigger
                    SensorDef(
                        bodyAnchorPoint = vec2(1f, -0.9f),
                        rayLengthOffset = vec2(1.5f, 0f),
                        type = SensorType.WALL_SENSOR,
                        name = "minotaur_wall",
                        color = Color.BLUE,
                        hitFilter = {
                            it.entityCategory == EntityCategory.GROUND ||
                                it.entityCategory == EntityCategory.WORLD_BOUNDARY
                        },
                    ),
                    // Wall Height Sensor: Checks higher up if the wall is jumpable (Minotaur jumps higher)
                    SensorDef(
                        bodyAnchorPoint = vec2(1f, 2.5f),
                        rayLengthOffset = vec2(1.5f, 0f),
                        type = SensorType.WALL_HEIGHT_SENSOR,
                        name = "minotaur_wall_height",
                        color = Color.BLUE,
                        hitFilter = { it.entityCategory == EntityCategory.GROUND },
                    ),
                    // Ground Sensor: Detects ground/ledges directly in front
                    SensorDef(
                        bodyAnchorPoint = vec2(1f, -1f),
                        rayLengthOffset = vec2(0f, -2.0f),
                        type = SensorType.GROUND_DETECT_SENSOR,
                        name = "minotaur_ground",
                        color = Color.GREEN,
                    ),
                    // Jump Sensor: Checks for landing spots further away due to higher speed/jump power
                    SensorDef(
                        bodyAnchorPoint = vec2(4.5f, -1f),
                        rayLengthOffset = vec2(0f, -2.0f),
                        type = SensorType.JUMP_SENSOR,
                        name = "minotaur_jump",
                        color = Color.GREEN,
                    ),
                    // Attack Sensor: Detects the player in a larger melee range
                    SensorDef(
                        bodyAnchorPoint = vec2(-0.1f, -0.7f),
                        rayLengthOffset = vec2(4.5f, 0f),
                        type = SensorType.ATTACK_SENSOR,
                        name = "minotaur_attack",
                        color = Color.ORANGE,
                        hitFilter = { it.entityCategory == EntityCategory.PLAYER },
                    ),
                ),
            sightSensorDefinition =
                SensorDef(
                    bodyAnchorPoint = vec2(0f, 0f),
                    rayLengthOffset = vec2(0f, 0f),
                    type = SensorType.SIGHT_SENSOR,
                    name = "mushroom_sight",
                    color = Color.WHITE,
                    hitFilter = { it.entityCategory == EntityCategory.GROUND },
                ),
        )
}
