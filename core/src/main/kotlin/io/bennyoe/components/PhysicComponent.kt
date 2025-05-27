package io.bennyoe.components

import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Shape2D
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.Body
import com.badlogic.gdx.physics.box2d.BodyDef
import com.badlogic.gdx.physics.box2d.World
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import com.github.quillraven.fleks.Entity
import io.bennyoe.config.EntityCategory
import io.bennyoe.config.GameConstants.UNIT_SCALE
import io.bennyoe.utility.BodyData
import io.bennyoe.utility.FixtureData
import io.bennyoe.utility.FixtureType
import ktx.app.gdxError
import ktx.box2d.body
import ktx.box2d.box
import ktx.box2d.loop
import ktx.log.logger
import ktx.math.vec2
import com.github.quillraven.fleks.World as entityWorld

class PhysicComponent : Component<PhysicComponent> {
    val offset: Vector2 = Vector2()
    val size: Vector2 = Vector2()
    var prevPos: Vector2 = Vector2()
    var impulse: Vector2 = Vector2()
    var categoryBits = EntityCategory.GROUND.bit
    var activeGroundContacts: Int = 0
    lateinit var body: Body

    override fun type() = PhysicComponent

    override fun entityWorld.onRemove(entity: Entity) {
        body.world.destroyBody(body)
    }

    companion object : ComponentType<PhysicComponent>() {
        fun physicsComponentFromShape2D(
            phyWorld: World,
            shape: Shape2D,
            x: Int = 0,
            y: Int = 0,
            myFriction: Float = 0f,
            setUserData: BodyData? = null,
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
                                position.set(bodyX, bodyY)
                                fixedRotation = true
                                allowSleep = false
                                userData = setUserData
                                loop(
                                    vec2(0f, 0f),
                                    vec2(bodyW, 0f),
                                    vec2(bodyW, bodyH),
                                    vec2(0f, bodyH),
                                ) {
                                    friction = myFriction
                                }
                            }
                    }
                }

                else -> gdxError("No valid shape for creating a physics component given")
            }
        }

        fun physicsComponentFromImage(
            phyWorld: World,
            image: Image,
            bodyType: BodyDef.BodyType = BodyDef.BodyType.DynamicBody,
            scalePhysicX: Float = 1f,
            scalePhysicY: Float = 1f,
            offsetX: Float = 0f,
            offsetY: Float = 0f,
            categoryBit: Short = 0x0001,
            fixedRotation: Boolean = true,
            allowSleep: Boolean = true,
            isSensor: Boolean = false,
            setUserdata: BodyData? = null,
            myFriction: Float = 1f,
        ): PhysicComponent {
            val x = image.x
            val y = image.y
            val width = image.width * scalePhysicX
            val height = image.height * scalePhysicY

            // create the Box2D body
            val body =
                phyWorld.body(bodyType) {
                    position.set(x + width * 0.5f, y + height * 0.5f)
                    this.fixedRotation = fixedRotation
                    this.allowSleep = allowSleep
                    userData = setUserdata
                }

            // fixture as box
            body.box(width, height, Vector2(offsetX, offsetY)) {
                this.isSensor = isSensor
                // is currentlu only use for creating player and enemy entities. If this changes the fixture userData must be generated dynamically
                this.userData = FixtureData(FixtureType.HITBOX_SENSOR)
                this.filter.categoryBits = categoryBit
                density = 1f
                friction = myFriction
            }

            return PhysicComponent().apply {
                this.body = body
                this.size.set(width, height)
                this.offset.set(offsetX, offsetY)
            }
        }

        val logger = logger<PhysicComponent>()
    }
}
