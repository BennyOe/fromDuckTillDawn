package io.bennyoe.config.entities

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.physics.box2d.BodyDef
import io.bennyoe.components.Attack
import io.bennyoe.components.AttackType
import io.bennyoe.components.ai.SensorDef
import io.bennyoe.components.animation.AnimationModel
import io.bennyoe.components.animation.SpectorAnimation
import io.bennyoe.config.CharacterType
import io.bennyoe.config.EntityCategory
import io.bennyoe.config.GameConstants.CHASE_DETECTION_RADIUS
import io.bennyoe.config.GameConstants.CHASE_SPEED
import io.bennyoe.config.GameConstants.NORMAL_DETECTION_RADIUS
import io.bennyoe.config.SpawnCfgFactory
import io.bennyoe.systems.render.ZIndex
import io.bennyoe.utility.SensorType
import ktx.math.vec2
import kotlin.experimental.or

object SpectorCfg {
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
            animationModel = AnimationModel.ENEMY_SPECTOR,
            animationType = SpectorAnimation.IDLE,
            bodyType = BodyDef.BodyType.DynamicBody,
            characterType = CharacterType.SPECTOR,
            canAttack = true,
            maxSightRadius = 7f,
            attackMap =
                mapOf(
                    AttackType.SWORD to
                        Attack(
                            AttackType.SWORD,
                            CharacterType.SPECTOR,
                            5f,
                            5f,
                            1f,
                            0.3f,
                        ),
                ),
            aiTreePath = "ai/spector.tree",
            scaleSpeed = 0.5f,
            keepCorpse = true,
            scalePhysic = vec2(0.3f, 0.4f),
            offsetPhysic = vec2(0f, -0.05f),
            removeDelay = 2f,
            nearbyEnemiesDefaultSensorRadius = NORMAL_DETECTION_RADIUS,
            nearbyEnemiesExtendedSensorRadius = CHASE_DETECTION_RADIUS,
            nearbyEnemiesSensorOffset = vec2(0f, 0f),
            chaseSpeed = CHASE_SPEED,
            zIndex = ZIndex.ENEMY_OFFSET.value,
            hearingRadius = 10f,
            basicSensorList =
                listOf(
                    // Wall Sensor: Checks for walls in front of the entity
                    SensorDef(
                        bodyAnchorPoint = vec2(1f, -0.8f),
                        rayLengthOffset = vec2(0.5f, 0f),
                        type = SensorType.WALL_SENSOR,
                        name = "spector_wall",
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
                        name = "spector_wall_height",
                        color = Color.BLUE,
                        hitFilter = { it.entityCategory == EntityCategory.GROUND },
                    ),
                    // Ground Sensor: Detects the ground (or lack thereof/ledge) in front
                    SensorDef(
                        bodyAnchorPoint = vec2(1f, -1f),
                        rayLengthOffset = vec2(0f, -1.4f),
                        type = SensorType.GROUND_DETECT_SENSOR,
                        name = "spector_ground",
                        color = Color.GREEN,
                    ),
                    // Jump Sensor: Checks for a landing spot across a gap
                    SensorDef(
                        bodyAnchorPoint = vec2(3.2f, -1f),
                        rayLengthOffset = vec2(0f, -1.4f),
                        type = SensorType.JUMP_SENSOR,
                        name = "spector_jump",
                        color = Color.GREEN,
                    ),
                    // Attack Sensor: Detects the player in melee range
                    SensorDef(
                        bodyAnchorPoint = vec2(1f, -0.7f),
                        rayLengthOffset = vec2(1f, 0f),
                        type = SensorType.ATTACK_SENSOR,
                        name = "spector_attack",
                        color = Color.ORANGE,
                        hitFilter = { it.entityCategory == EntityCategory.PLAYER },
                    ),
                ),
        )
}
