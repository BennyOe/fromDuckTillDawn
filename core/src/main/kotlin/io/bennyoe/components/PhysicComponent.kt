package io.bennyoe.components

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.Body
import com.badlogic.gdx.physics.box2d.BodyDef
import com.badlogic.gdx.physics.box2d.World
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import com.github.quillraven.fleks.Entity
import ktx.box2d.body
import ktx.box2d.box
import com.github.quillraven.fleks.World as entityWorld

class PhysicComponent() : Component<PhysicComponent> {
    private val offset: Vector2 = Vector2()
    private val size: Vector2 = Vector2()
    lateinit var body: Body


    override fun type() = PhysicComponent

    companion object : ComponentType<PhysicComponent>() {
        fun physicsComponentFromImage(
            world: World,
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

            // Erstelle den Box2D-Körper
            val body = world.body(bodyType) {
                position.set(x + width * 0.5f, y + height * 0.5f)
                this.fixedRotation = fixedRotation
                this.allowSleep = allowSleep
            }

            // Standardmäßig wird eine rechteckige Fixture erstellt
            body.box(width, height, Vector2(offsetX, offsetY)) {
                this.isSensor = isSensor
                this.userData = "IMAGE_HITBOX"
                this.filter.categoryBits = categoryBit
            }

            // Erstelle das PhysicsComponent-Objekt und speichere den Körper darin
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

