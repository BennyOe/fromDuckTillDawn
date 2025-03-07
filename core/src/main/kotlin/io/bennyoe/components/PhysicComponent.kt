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
import io.bennyoe.Duckee.Companion.UNIT_SCALE
import ktx.app.gdxError
import ktx.box2d.body
import ktx.box2d.box
import ktx.box2d.loop
import ktx.math.vec2
import com.github.quillraven.fleks.World as entityWorld

class PhysicComponent() : Component<PhysicComponent> {
    private val offset: Vector2 = Vector2()
    private val size: Vector2 = Vector2()
    var prevPos: Vector2 = Vector2()
    var impulse: Vector2 = Vector2()
    lateinit var body: Body

    override fun type() = PhysicComponent

    companion object : ComponentType<PhysicComponent>() {
        fun physicsComponentFromShape2D(
            phyWorld: World,
            x: Int,
            y: Int,
            shape: Shape2D,
            myFriction: Float = 0f
        ): PhysicComponent {
            when (shape) {
                is Rectangle -> {
                    val bodyX = x + shape.x * UNIT_SCALE
                    val bodyY = y + shape.y * UNIT_SCALE
                    val bodyW = shape.width * UNIT_SCALE
                    val bodyH = shape.height * UNIT_SCALE
                    return PhysicComponent().apply {
                        body = phyWorld.body(BodyDef.BodyType.StaticBody) {
                            position.set(bodyX, bodyY)
                            fixedRotation = true
                            allowSleep = false
                            loop(
                                vec2(0f, 0f),
                                vec2(bodyW, 0f),
                                vec2(bodyW, bodyH),
                                vec2(0f, bodyH)
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
        ): PhysicComponent {
            val x = image.x
            val y = image.y
            val width = image.width * scalePhysicX
            val height = image.height * scalePhysicY

            // create the Box2D body
            val body = phyWorld.body(bodyType) {
                position.set(x + width * 0.5f, y + height * 0.5f)
                this.fixedRotation = fixedRotation
                this.allowSleep = allowSleep
            }

            // fixture as box
            body.box(width, height, Vector2(offsetX, offsetY)) {
                this.isSensor = isSensor
                this.userData = "IMAGE_HITBOX"
                this.filter.categoryBits = categoryBit
            }

            return PhysicComponent().apply {
                this.body = body
                this.size.set(width, height)
                this.offset.set(offsetX, offsetY)
            }
        }
    }

    override fun entityWorld.onAdd(entity: Entity) {
        body.userData = entity
    }

    override fun entityWorld.onRemove(entity: Entity) {
        body.world.destroyBody(body)
        body.userData = null
    }
}
