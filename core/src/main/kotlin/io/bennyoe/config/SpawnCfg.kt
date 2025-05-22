package io.bennyoe.config

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.BodyDef
import io.bennyoe.components.AnimationModel
import io.bennyoe.components.AnimationType
import io.bennyoe.components.AnimationVariant
import ktx.math.vec2

data class SpawnCfg(
    val animationModel: AnimationModel = AnimationModel.NONE,
    val animationType: AnimationType = AnimationType.NONE,
    val animationVariant: AnimationVariant = AnimationVariant.NONE,
    val bodyType: BodyDef.BodyType = BodyDef.BodyType.StaticBody,
    val entityCategory: Short = EntityCategory.WALL.bit,
    val canAttack: Boolean = false,
    val scaleAttackDamage: Float = 1f,
    val attackExtraRange: Float = 1f,
    val scalePhysic: Vector2 = vec2(1f, 1f),
    val offsetPhysic: Vector2 = vec2(0f, 0f),
    val scaleImage: Vector2 = vec2(1f, 1f),
    val scaleSpeed: Float = 1f,
)
