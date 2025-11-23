package io.bennyoe.state.minotaur

import com.badlogic.gdx.ai.msg.MessageManager
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.BodyDef
import com.badlogic.gdx.physics.box2d.Joint
import com.badlogic.gdx.physics.box2d.joints.WeldJointDef
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
import io.bennyoe.components.ProjectileType
import io.bennyoe.components.TransformComponent
import io.bennyoe.components.WalkDirection
import io.bennyoe.components.ai.RayHitComponent
import io.bennyoe.config.EntityCategory
import io.bennyoe.config.GameConstants.GRAVITY
import io.bennyoe.state.AbstractStateContext
import io.bennyoe.state.FsmMessageTypes
import io.bennyoe.utility.EntityBodyData
import ktx.math.vec2
import kotlin.experimental.or
import com.badlogic.gdx.physics.box2d.World as PhyWorld

private const val MINOTAUR_GRABBING_OFFSET = 6f

private const val PLAYER_THROW_DISTANCE = 6

class MinotaurStateContext(
    entity: Entity,
    world: World,
    val phyWorld: PhyWorld,
    stage: Stage,
    val minotaurAtlas: TextureAtlas,
    deltaTime: Float = 0f,
) : AbstractStateContext<MinotaurStateContext>(entity, world, stage, deltaTime) {
    private val messageDispatcher = MessageManager.getInstance()
    private var grabbingJoint: Joint? = null
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
        val spawnPos = transformCmp.position.cpy().add(facingDir, 6.5f)
        val width = 4.7f
        val height = 4.7f

        heldProjectile =
            world.entity {
                it += TransformComponent(spawnPos, width, height)
                val imageCmp =
                    ImageComponent(stage).apply {
                        image = Image(minotaurAtlas.findRegion("rock"))
                        image.setPosition(spawnPos.x - width * 0.5f, spawnPos.y)
                        image.setSize(width, height)
                        zIndex = 100000
                    }
                it += imageCmp

                val phyBody =
                    PhysicComponent.physicsComponentFromBox(
                        phyWorld = phyWorld,
                        entity = it,
                        centerPos = spawnPos,
                        width,
                        height,
                        bodyType = BodyDef.BodyType.KinematicBody,
                        setUserdata = EntityBodyData(it, EntityCategory.ENEMY_PROJECTILE),
                        categoryBit = EntityCategory.ENEMY_PROJECTILE.bit,
                        maskBit =
                            EntityCategory.GROUND.bit or
                                EntityCategory.WORLD_BOUNDARY.bit or
                                EntityCategory.PLAYER.bit or
                                EntityCategory.WATER.bit or
                                EntityCategory.SENSOR.bit,
                    )
                it += phyBody
                it += ProjectileComponent(24f, ProjectileType.ROCK)
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

        // 2. Start-/Zielposition
        val startPos = rockPhyCmp.body.position.cpy()

        // MODIFIED! Wir benutzen explizit die Spielerposition als Ziel
        val targetX = targetPos.x
        val targetY = targetPos.y

        val dx = targetX - startPos.x
        if (dx == 0f) {
            return
        }

        // Wie lange der Stein in der Luft sein soll (in Sekunden) – Tweak-Parameter fürs Gameplay
        // Je größer, desto höher/weiter der Bogen
        val flightTime = 0.4f

        // 3. Horizontal: x(T) = x0 + vx * T  →  vx = (xt - x0) / T
        val vx = dx / flightTime

        // 4. Vertikal: y(T) = y0 + vy * T + 0.5 * a * T^2
        //    → vy = (yt - y0 - 0.5 * a * T^2) / T
        val dy = targetY - startPos.y
        val vy = (dy - 0.5f * GRAVITY * flightTime * flightTime) / flightTime

        // 5. Gewünschte Geschwindigkeit → Impuls
        //    impulse = (v_desired - v_current) * mass
        val desiredVelocity = Vector2(vx, vy)
        val currentVelocity = rockPhyCmp.body.linearVelocity
        val velocityChange = desiredVelocity.sub(currentVelocity)
        val impulse = velocityChange.scl(rockPhyCmp.body.mass)

        rockPhyCmp.body.applyLinearImpulse(impulse, rockPhyCmp.body.worldCenter, true)

        // 6. Referenz loslassen
        heldProjectile = null
    }

    fun getPlayerPosition(): Vector2 {
        val playerTransformCmp = with(world) { playerEntity[TransformComponent] }
        return playerTransformCmp.position
    }

    fun grabPlayer() {
        if (grabbingJoint != null) return
        val playerIntentionCmp = with(world) { playerEntity[IntentionComponent] }
        val playerPhysicCmp = with(world) { playerEntity[PhysicComponent] }
        playerIntentionCmp.isBeingGrabbed = true
        messageDispatcher.dispatchMessage(FsmMessageTypes.PLAYER_IS_GRABBED.ordinal)

        val direction = if (imageCmp.flipImage) -1 else 1

        grabbingJoint =
            phyWorld.createJoint(
                WeldJointDef().apply {
                    bodyA = physicComponent.body
                    bodyB = playerPhysicCmp.body
                    collideConnected = false
                    localAnchorA.set(direction * MINOTAUR_GRABBING_OFFSET, 0f)
                },
            )
    }

    fun releasePlayer() {
        val playerIntentionCmp = with(world) { playerEntity[IntentionComponent] }
        val playerPhysicCmp = with(world) { playerEntity[PhysicComponent] }

        playerIntentionCmp.isBeingGrabbed = false
        grabbingJoint?.let { joint ->
            phyWorld.destroyJoint(joint)
            grabbingJoint = null
            playerIntentionCmp.isThrown = true

            val direction = if (imageCmp.flipImage) -1 else 1
            val startPos = playerPhysicCmp.body.position.cpy()
            val targetX = startPos.x + PLAYER_THROW_DISTANCE * direction

            // Horizontal distance
            val dx = targetX - startPos.x

            val flightTime = 0.4f

            // Calculate required velocities (Ballistic trajectory)
            // We assume dy = 0 (throw to same height), or slightly upwards arc
            // dy = 0 in the formula: vy = -0.5 * g * t
            val vx = dx / flightTime
            val vy = -0.5f * GRAVITY * flightTime

            val desiredVelocity = vec2(vx, vy)
            val currentVelocity = playerPhysicCmp.body.linearVelocity
            val velocityChange = desiredVelocity.sub(currentVelocity)
            val impulse = velocityChange.scl(playerPhysicCmp.body.mass)

            playerPhysicCmp.impulse.set(impulse)
        }
    }
}
