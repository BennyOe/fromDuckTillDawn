package io.bennyoe.systems.entitySpawn

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.maps.MapLayer
import com.badlogic.gdx.maps.tiled.objects.TiledMapTileMapObject
import com.badlogic.gdx.maps.tiled.tiles.AnimatedTiledMapTile
import com.badlogic.gdx.physics.box2d.BodyDef
import com.badlogic.gdx.physics.box2d.joints.RopeJoint
import com.badlogic.gdx.physics.box2d.joints.RopeJointDef
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.github.quillraven.fleks.World
import io.bennyoe.components.AnimationComponent
import io.bennyoe.components.ChainRenderComponent
import io.bennyoe.components.ImageComponent
import io.bennyoe.components.LightComponent
import io.bennyoe.components.ParticleComponent
import io.bennyoe.components.ParticleType
import io.bennyoe.components.PhysicComponent
import io.bennyoe.components.TransformComponent
import io.bennyoe.components.audio.AudioComponent
import io.bennyoe.config.EntityCategory
import io.bennyoe.config.GameConstants.UNIT_SCALE
import io.bennyoe.lightEngine.core.LightEffectType
import io.bennyoe.lightEngine.core.Scene2dLightEngine
import io.bennyoe.systems.audio.SoundType
import ktx.box2d.body
import ktx.box2d.box
import ktx.math.vec2
import ktx.tiled.type
import kotlin.experimental.or

class MapObjectSpawner(
    val world: World,
    val stage: Stage,
    val phyWorld: com.badlogic.gdx.physics.box2d.World,
    val lightEngine: Scene2dLightEngine,
    val worldObjectsAtlas: TextureAtlas,
) {
    fun spawnMapObjects(
        mapObjectsLayer: MapLayer,
        layerZIndex: Int,
    ) {
        mapObjectsLayer.objects.forEach { mapObject ->
            mapObject as TiledMapTileMapObject

            if (mapObject.type == "lantern") {
                createLanternFromMapObject(mapObject, layerZIndex)
                return@forEach
            }

            world.entity {
                val zIndex = mapObject.properties.get("zIndex", Int::class.java) ?: 0
                val width =
                    mapObject.tile.textureRegion.regionWidth
                        .toFloat() * UNIT_SCALE
                val height =
                    mapObject.tile.textureRegion.regionHeight
                        .toFloat() * UNIT_SCALE
                val image = ImageComponent(stage, zIndex = layerZIndex + zIndex)
                image.image = Image(mapObject.tile.textureRegion)
                it += image

                if (mapObject.properties.get("sound") != null) {
                    it +=
                        AudioComponent(
                            SoundType.valueOf(mapObject.properties.get("sound", String::class.java).uppercase()),
                            mapObject.properties.get("soundVolume", Float::class.java) ?: .5f,
                            mapObject.properties.get("soundAttenuationMaxDistance", Float::class.java) ?: 10f,
                            mapObject.properties.get("soundAttenuationMinDistance", Float::class.java) ?: 1f,
                            mapObject.properties.get("soundAttenuationFactor", Float::class.java) ?: 1f,
                        )
                }

                if (mapObject.tile is AnimatedTiledMapTile) {
                    val animatedTile = mapObject.tile as AnimatedTiledMapTile
                    val frameInterval = 64f / 1000f
                    val frames = animatedTile.frameTiles.map { tile -> TextureRegionDrawable(tile.textureRegion) }
                    val animation = Animation(frameInterval, *frames.toTypedArray())
                    animation.playMode = Animation.PlayMode.LOOP

                    val aniCmp = AnimationComponent()
                    aniCmp.animation = animation
                    it += aniCmp
                }

                it +=
                    TransformComponent(
                        vec2(mapObject.x * UNIT_SCALE, mapObject.y * UNIT_SCALE),
                        width,
                        height,
                    )
                when (mapObject.type) {
                    "fire" -> {
                        // Add ParticleComponent for fire if the map object type is "fire"
                        it +=
                            ParticleComponent(
                                particleFile = Gdx.files.internal("particles/fire.p"),
                                scaleFactor = 1f / 82f,
                                motionScaleFactor = 1f / 50f,
                                looping = true,
                                zIndex = layerZIndex + zIndex,
                                offsetX = width * 0.5f,
                                offsetY = 0.2f,
                                stage = stage,
                                type = ParticleType.FIRE,
                            )
                    }
                }
            }
        }
    }

    private fun createLanternFromMapObject(
        mapObject: TiledMapTileMapObject,
        layerZIndex: Int,
    ) {
        world.entity { e ->
            val zIndex = (mapObject.properties.get("zIndex", Int::class.java) ?: 0) + layerZIndex

            val scale = 0.4f // The scale you originally used for physics.
            val visualWidth = (mapObject.tile.textureRegion.regionWidth * UNIT_SCALE) * scale
            val visualHeight = (mapObject.tile.textureRegion.regionHeight * UNIT_SCALE) * scale
            val position = vec2(mapObject.x * UNIT_SCALE, mapObject.y * UNIT_SCALE)

            // 2. Create ImageComponent with the final size and correct origin.
            val (image, maybeAnimation) = buildImageAndOptionalAnimation(mapObject)
            image.setSize(visualWidth, visualHeight)
            image.setOrigin(visualWidth / 2f, visualHeight / 2f) // Set origin to the center of the scaled image.

            val imageCmp = ImageComponent(stage, zIndex = zIndex)
            imageCmp.image = image
            e += imageCmp

            maybeAnimation?.let { anim ->
                val aniCmp = AnimationComponent().apply { animation = anim }
                e += aniCmp
            }

            e += TransformComponent(position, visualWidth, visualHeight)

            // 3. Create the anchor body for the chain.
            val anchorBody =
                phyWorld.body(BodyDef.BodyType.StaticBody) {
                    this.position.set(position.x + visualWidth * 0.5f, position.y + visualHeight + 0.5f) // Anchor position adjusted
                }

            // 4. Create the lantern's physics body with the SAME size as the image.
            val phyCmp =
                PhysicComponent().apply {
                    body =
                        phyWorld.body(BodyDef.BodyType.DynamicBody) {
                            this.position.set(position.x + visualWidth * 0.5f, position.y + visualHeight * 0.5f)
                            fixedRotation = false
                            allowSleep = true
                            angularDamping = 1f
                            linearDamping = 0.6f
                        }
                    body.box(visualWidth, visualHeight) {
                        density = 10f
                        friction = 1.5f
                        filter.categoryBits = EntityCategory.LANTERN.bit
                        filter.maskBits = EntityCategory.PLAYER.bit or EntityCategory.GROUND.bit
                    }
                    this.size.set(visualWidth, visualHeight)
                }
            e += phyCmp

            // 5. Create the RopeJoint, connecting the anchor to the top-center of the lantern body.
            val ropeJointDef =
                RopeJointDef().apply {
                    bodyA = anchorBody
                    bodyB = phyCmp.body
                    localAnchorA.set(0f, 0f)
                    localAnchorB.set(0f, visualHeight * 0.5f) // Top-center of the physics body.
                    maxLength = 3.8f // A bit longer than the anchor distance to allow some swing.
                    collideConnected = true
                }
            val joint = phyWorld.createJoint(ropeJointDef) as RopeJoint

            e +=
                ChainRenderComponent(
                    joint = joint,
                    bodyA = anchorBody,
                    bodyB = phyCmp.body,
                    texture = worldObjectsAtlas.findRegion("chain"),
                    segmentHeight = 1f,
                )

            // add light to body
            val light =
                lightEngine.addPointLight(
                    phyCmp.body.position,
                    Color.ORANGE,
                    5f,
                    12f,
                    2f,
                    1f,
                    rays = 512,
                )
            light.effect = LightEffectType.OIL_LAMP
            e += LightComponent(light)
        }
    }

    private fun buildImageAndOptionalAnimation(mapObject: TiledMapTileMapObject): Pair<Image, Animation<TextureRegionDrawable>?> {
        val baseRegion = mapObject.tile.textureRegion
        val width = baseRegion.regionWidth.toFloat() * UNIT_SCALE
        val height = baseRegion.regionHeight.toFloat() * UNIT_SCALE

        return if (mapObject.tile is AnimatedTiledMapTile) {
            val animatedTile = mapObject.tile as AnimatedTiledMapTile
            val frameInterval = 64f / 1000f
            val frames = animatedTile.frameTiles.map { tile -> TextureRegionDrawable(tile.textureRegion) }
            val animation =
                Animation(frameInterval, *frames.toTypedArray()).apply {
                    playMode = Animation.PlayMode.LOOP
                }
            // start frame
            val image =
                Image(frames.first()).apply {
                    setSize(width, height)
                    // don't know why the multiplier has to be 0.2
                    setOrigin(width * 0.2f, height * 0.2f)
                    setPosition(mapObject.x * UNIT_SCALE, mapObject.y * UNIT_SCALE)
                }
            image to animation
        } else {
            val image =
                Image(baseRegion).apply {
                    setSize(width, height)
                    setOrigin(width * 0.5f, height * 0.5f)
                    setPosition(mapObject.x * UNIT_SCALE, mapObject.y * UNIT_SCALE)
                }
            image to null
        }
    }
}
