package io.bennyoe.components

import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Shape2D
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.Body
import com.badlogic.gdx.physics.box2d.BodyDef
import com.badlogic.gdx.physics.box2d.World
import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import com.github.quillraven.fleks.Entity
import io.bennyoe.config.EntityCategory
import io.bennyoe.config.GameConstants
import io.bennyoe.config.GameConstants.UNIT_SCALE
import io.bennyoe.utility.EntityBodyData
import io.bennyoe.utility.FixtureSensorData
import io.bennyoe.utility.FloorType
import io.bennyoe.utility.SensorType
import ktx.app.gdxError
import ktx.box2d.body
import ktx.box2d.box
import ktx.box2d.loop
import ktx.log.logger
import ktx.math.vec2
import com.github.quillraven.fleks.World as entityWorld

const val WATER_CONTACT_GRACE_PERIOD = 3f * GameConstants.PHYSIC_TIME_STEP
const val AIR_BUBBLES_START_DELAY = 3f

class PhysicComponent : Component<PhysicComponent> {
    val offset: Vector2 = Vector2()
    val size: Vector2 = Vector2()
    var prevPos: Vector2 = Vector2()
    var prevAngle: Float = 0f
    var impulse: Vector2 = Vector2()
    var categoryBits = EntityCategory.GROUND.bit
    var activeGroundContacts: Int = 0
    var activeWaterContacts: Int = 0
    var activeUnderWaterContacts: Int = 0
    var waterContactGraceTimer: Float = 0f
    var underWaterGraceTimer: Float = 0f
    var airBubblesDelayTimer: Float = 0f
    var floorType: FloorType? = null
    lateinit var body: Body

    override fun type() = PhysicComponent

    override fun entityWorld.onRemove(entity: Entity) {
        body.world.destroyBody(body)
    }

    companion object : ComponentType<PhysicComponent>() {
        fun physicsComponentFromShape2D(
            phyWorld: World,
            entity: Entity,
            shape: Shape2D,
            x: Int = 0,
            y: Int = 0,
            myFriction: Float = 0f,
            setUserData: EntityBodyData? = null,
            isSensor: Boolean = false,
            sensorType: SensorType = SensorType.NONE,
            categoryBit: Short = EntityCategory.GROUND.bit,
            maskBit: Short = -1,
            myDensity: Float = 1f,
        ): PhysicComponent {
            when (shape) {
                is Rectangle -> {
                    val bodyX = x + shape.x * UNIT_SCALE
                    val bodyY = y + shape.y * UNIT_SCALE
                    val bodyW = shape.width * UNIT_SCALE
                    val bodyH = shape.height * UNIT_SCALE
                    return PhysicComponent().apply {
                        body =
                            phyWorld.body(BodyDef.BodyType.StaticBody) {
                                position.set(bodyX + bodyW * 0.5f, bodyY + bodyH * 0.5f)
                                fixedRotation = true
                                allowSleep = false
                                userData = setUserData

                                // Use a box shape for the sensor to detect the entire area, not just the vertices
                                if (isSensor) {
                                    box(bodyW, bodyH) {
                                        this.isSensor = true
                                        this.userData = FixtureSensorData(entity, sensorType)
                                        filter.categoryBits = categoryBit
                                        filter.maskBits = maskBit
                                        density = 1f
                                        friction = myFriction
                                    }
                                } else {
                                    val halfW = bodyW * 0.5f
                                    val halfH = bodyH * 0.5f
                                    loop(
                                        vec2(-halfW, -halfH),
                                        vec2(halfW, -halfH),
                                        vec2(halfW, halfH),
                                        vec2(-halfW, halfH),
                                    ) {
                                        this.isSensor = false
                                        this.userData = FixtureSensorData(entity, sensorType)
                                        filter.categoryBits = categoryBit
                                        filter.maskBits = maskBit
                                        this.density = myDensity
                                        friction = myFriction
                                    }
                                }
                            }
                    }
                }

                else -> {
                    gdxError("No valid shape for creating a physics component given")
                }
            }
        }

        fun physicsComponentFromBox(
            phyWorld: World,
            entity: Entity,
            centerPos: Vector2,
            width: Float,
            height: Float,
            bodyType: BodyDef.BodyType = BodyDef.BodyType.DynamicBody,
            scalePhysicX: Float = 1f,
            scalePhysicY: Float = 1f,
            offsetX: Float = 0f,
            offsetY: Float = 0f,
            categoryBit: Short = EntityCategory.ENEMY.bit,
            maskBit: Short = -1,
            fixedRotation: Boolean = true,
            allowSleep: Boolean = true,
            isSensor: Boolean = false,
            setUserdata: EntityBodyData? = null,
            myFriction: Float = 1f,
            sensorType: SensorType = SensorType.NONE,
            myDensity: Float = 1f,
        ): PhysicComponent {
            val scaledWidth = width * scalePhysicX
            val scaledHeight = height * scalePhysicY

            // create the Box2D body
            val body =
                phyWorld.body(bodyType) {
                    position.set(centerPos)
                    this.fixedRotation = fixedRotation
                    this.allowSleep = allowSleep
                    userData = setUserdata
                }

            // fixture as box
            body.box(scaledWidth, scaledHeight, Vector2(offsetX, offsetY)) {
                this.isSensor = isSensor
                this.userData = FixtureSensorData(entity, sensorType)
                this.filter.categoryBits = categoryBit
                this.filter.maskBits = maskBit
                this.density = myDensity
                friction = myFriction
            }

            return PhysicComponent().apply {
                this.body = body
                this.size.set(scaledWidth, scaledHeight)
                this.offset.set(offsetX, offsetY)
            }
        }

        val logger = logger<PhysicComponent>()
    }
}
