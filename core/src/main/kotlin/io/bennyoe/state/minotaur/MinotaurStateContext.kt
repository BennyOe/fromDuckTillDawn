package io.bennyoe.state.minotaur

import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.BodyDef
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import io.bennyoe.components.AttackComponent
import io.bennyoe.components.ImageComponent
import io.bennyoe.components.IntentionComponent
import io.bennyoe.components.PhysicComponent
import io.bennyoe.components.PlayerComponent
import io.bennyoe.components.ProjectileComponent
import io.bennyoe.components.TransformComponent
import io.bennyoe.components.WalkDirection
import io.bennyoe.components.ai.RayHitComponent
import io.bennyoe.config.EntityCategory
import io.bennyoe.state.AbstractStateContext
import com.badlogic.gdx.physics.box2d.World as PhyWorld

class MinotaurStateContext(
    entity: Entity,
    world: World,
    val phyWorld: PhyWorld,
    stage: Stage,
    val minotaurAtlas: TextureAtlas,
    deltaTime: Float = 0f,
) : AbstractStateContext<MinotaurStateContext>(entity, world, stage, deltaTime) {
    val intentionCmp: IntentionComponent by lazy { with(world) { entity[IntentionComponent] } }
    val attackCmp: AttackComponent by lazy { with(world) { entity[AttackComponent] } }
    val imageCmp: ImageComponent by lazy { with(world) { entity[ImageComponent] } }
    val playerEntity = world.family { all(PlayerComponent, PhysicComponent) }.first()
    val rayHitCmp: RayHitComponent by lazy { with(world) { entity[RayHitComponent] } }

    override val wantsToJump get() = intentionCmp.wantsToJump
    override val wantsToAttack get() = intentionCmp.wantsToAttack
    val wantsToScream get() = intentionCmp.wantsToScream
    val wantsToGrabAttack get() = intentionCmp.wantsToGrabAttack
    val wantsToThrowAttack get() = intentionCmp.wantsToThrowAttack
    val wantsToStompAttack get() = intentionCmp.wantsToStomp

    var heldProjectile: Entity? = null

    fun rotateToPlayer() {
        val playerTransformCmp = with(world) { playerEntity[TransformComponent] }
        imageCmp.flipImage = if (transformCmp.position.x < playerTransformCmp.position.x) false else true
    }

    fun runTowardsPlayer() {
        val playerTransformCmp = with(world) { playerEntity[TransformComponent] }
        val direction = if (playerTransformCmp.position.x < transformCmp.position.x) WalkDirection.LEFT else WalkDirection.RIGHT
        intentionCmp.walkDirection = direction
        intentionCmp.wantsToChase = true
    }

    fun stopMovement() {
        intentionCmp.walkDirection = WalkDirection.NONE
    }

    fun runIntoWall(): Boolean = rayHitCmp.wallHit

    fun resetAllIntentions() {
        intentionCmp.resetAllIntentions()
    }

    fun runIntoPlayer(): Boolean = rayHitCmp.canAttack

    fun spawnRock() {
        val facingDir = if (imageCmp.flipImage) -1f else 1f
        val spawnPos = transformCmp.position.cpy().add(1.5f * facingDir, 0.5f)
        val width = 4f
        val height = 4f

        heldProjectile =
            world.entity {
                it += TransformComponent(spawnPos, width, height)
                val imageCmp =
                    ImageComponent(stage).apply {
                        image = Image(minotaurAtlas.findRegion("rock"))
                        image.setPosition(spawnPos.x - width * 0.5f, spawnPos.y + transformCmp.height - height - 1f)
                        image.setSize(width, height)
                        zIndex = 100000
                    }
                it += imageCmp

                val phyBody =
                    PhysicComponent.physicsComponentFromImage(
                        phyWorld = phyWorld,
                        entity = it,
                        image = imageCmp.image,
                        width,
                        height,
                        bodyType = BodyDef.BodyType.KinematicBody,
                    )
                it += phyBody
                it += ProjectileComponent(EntityCategory.ENEMY)
            }
    }

    fun throwRock(targetPos: Vector2) {
        val rock = heldProjectile ?: return
        val rockPhyCmp = with(world) { rock[PhysicComponent] }
        val projectileCmp = with(world) { rock[ProjectileComponent] }

        // 1. Physik aktivieren
        rockPhyCmp.body.type = BodyDef.BodyType.DynamicBody
        rockPhyCmp.body.isSleepingAllowed = true
        rockPhyCmp.body.isAwake = true
        projectileCmp.isThrown = true

        // 2. Impuls berechnen (einfache ballistische Kurve oder direkter Wurf)
        val throwStartPos = rockPhyCmp.body.position
        // Simpler Richtungsvektor + etwas Bogen nach oben
        val direction = targetPos.cpy().sub(throwStartPos).nor()
        val throwForce = 20f // Muss getweakt werden
        val impulse = Vector2(direction.x * throwForce, direction.y * throwForce + 5f)

        rockPhyCmp.body.applyLinearImpulse(impulse, rockPhyCmp.body.worldCenter, true)

        // 3. Referenz loslassen
        heldProjectile = null
    }

    fun getPlayerPosition(): Vector2 {
        val playerTransformCmp = with(world) { playerEntity[TransformComponent] }
        return playerTransformCmp.position
    }
}
