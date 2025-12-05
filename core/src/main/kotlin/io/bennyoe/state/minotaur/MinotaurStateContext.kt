package io.bennyoe.state.minotaur

import com.badlogic.gdx.ai.msg.MessageManager
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.BodyDef
import com.badlogic.gdx.physics.box2d.Joint
import com.badlogic.gdx.physics.box2d.joints.WeldJointDef
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import io.bennyoe.ai.blackboards.MushroomContext.Companion.TMP_CIRC
import io.bennyoe.components.AttackComponent
import io.bennyoe.components.HealthComponent
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
import io.bennyoe.event.CameraShakeEvent
import io.bennyoe.event.fire
import io.bennyoe.state.AbstractStateContext
import io.bennyoe.state.FsmMessageTypes
import io.bennyoe.systems.debug.DebugRenderer
import io.bennyoe.systems.debug.addToDebugView
import io.bennyoe.utility.EntityBodyData
import ktx.math.vec2
import kotlin.experimental.or
import com.badlogic.gdx.physics.box2d.World as PhyWorld

private const val MINOTAUR_GRABBING_OFFSET = 4.2f

private const val PLAYER_THROW_DISTANCE = 6
private const val STOMP_ATTACK_RADIUS = 10f
private const val SHOCKWAVE_VELOCITY = 30f

class MinotaurStateContext(
    entity: Entity,
    world: World,
    val phyWorld: PhyWorld,
    stage: Stage,
    val minotaurAtlas: TextureAtlas,
    deltaTime: Float = 0f,
    val debugRenderer: DebugRenderer,
) : AbstractStateContext<MinotaurStateContext>(entity, world, stage, deltaTime) {
    private val messageDispatcher = MessageManager.getInstance()
    private var grabbingJoint: Joint? = null
    private var chargeDirection: WalkDirection = WalkDirection.NONE
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

    fun prepareCharge() {
        val playerTransformCmp = with(world) { playerEntity[TransformComponent] }
        chargeDirection = if (playerTransformCmp.position.x < transformCmp.position.x) WalkDirection.LEFT else WalkDirection.RIGHT
    }

    fun chargeForward() {
        intentionCmp.walkDirection = chargeDirection
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

    fun spawnShockwave(playerPos: Vector2) {
        val spawnPos = transformCmp.position.cpy().add(0f, -5.5f)
        val width = 4.7f
        val height = 1.7f
        val xVelo = if (playerPos.x < spawnPos.x) -SHOCKWAVE_VELOCITY else SHOCKWAVE_VELOCITY

        stage.fire(CameraShakeEvent())

        world.entity {
            it += TransformComponent(spawnPos, width, height)
            val imageCmp =
                ImageComponent(stage).apply {
                    image = Image(minotaurAtlas.findRegion("shockwave"))
                    image.setPosition(spawnPos.x - width * 0.5f, spawnPos.y)
                    image.setSize(width, height)
                    zIndex = 100000
                    flipImage = xVelo < 0
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
                    isSensor = true,
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
            it += ProjectileComponent(24f, ProjectileType.SHOCKWAVE)
            phyBody.body.linearVelocity = vec2(xVelo, 0f)
        }
    }

    fun spawnRock() {
        val facingDir = if (imageCmp.flipImage) -1f else 1f
        val spawnPos = transformCmp.position.cpy().add(facingDir, 5.5f)
        val width = 3.7f
        val height = 3.7f

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

        rockPhyCmp.body.type = BodyDef.BodyType.DynamicBody
        rockPhyCmp.body.isSleepingAllowed = true
        rockPhyCmp.body.isAwake = true
        projectileCmp.isThrown = true

        val startPos = rockPhyCmp.body.position.cpy()

        val targetX = targetPos.x
        val targetY = targetPos.y

        val dx = targetX - startPos.x
        if (dx == 0f) {
            return
        }

        val flightTime = 0.4f

        // Horizontal: x(T) = x0 + vx * T  →  vx = (xt - x0) / T
        val vx = dx / flightTime

        // Vertical: y(T) = y0 + vy * T + 0.5 * a * T^2
        //    → vy = (yt - y0 - 0.5 * a * T^2) / T
        val dy = targetY - startPos.y
        val vy = (dy - 0.5f * GRAVITY * flightTime * flightTime) / flightTime

        //    impulse = (v_desired - v_current) * mass
        val desiredVelocity = Vector2(vx, vy)
        val currentVelocity = rockPhyCmp.body.linearVelocity
        val velocityChange = desiredVelocity.sub(currentVelocity)
        val impulse = velocityChange.scl(rockPhyCmp.body.mass)

        rockPhyCmp.body.applyLinearImpulse(impulse, rockPhyCmp.body.worldCenter, true)

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

    fun stompAttack() {
        intentionCmp.wantsToStomp = false
        stage.fire(CameraShakeEvent())

        if (isPlayerInStompRange()) {
            val playerHealthCmp = with(world) { playerEntity[HealthComponent] }
            playerHealthCmp.takenDamage = 10f
        }
    }

    private fun isPlayerInStompRange(): Boolean {
        val selfPos = physicComponent.body.position

        // draw the chase range
        TMP_CIRC
            .set(
                physicComponent.body.position.x,
                physicComponent.body.position.y,
                STOMP_ATTACK_RADIUS,
            )
        TMP_CIRC.addToDebugView(debugRenderer, Color.GREEN, "stompAttackRange")

        // calculate the distance to the player and return true if it is < chaseRange
        val playerPos = with(world) { playerEntity[PhysicComponent].body.position }
        val dist2 = selfPos.dst(playerPos)

        return dist2 <= STOMP_ATTACK_RADIUS
    }
}
