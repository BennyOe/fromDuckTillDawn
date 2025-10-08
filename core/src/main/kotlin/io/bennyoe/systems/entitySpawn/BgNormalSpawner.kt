package io.bennyoe.systems.entitySpawn

import com.badlogic.gdx.maps.MapLayer
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.github.quillraven.fleks.World
import io.bennyoe.assets.TextureAtlases
import io.bennyoe.components.ImageComponent
import io.bennyoe.components.ShaderRenderingComponent
import io.bennyoe.components.TiledTextureComponent
import io.bennyoe.components.TransformComponent
import io.bennyoe.config.GameConstants.UNIT_SCALE
import ktx.math.vec2
import ktx.tiled.height
import ktx.tiled.width
import ktx.tiled.x
import ktx.tiled.y

class BgNormalSpawner(
    private val world: World,
    private val stage: Stage,
    private val bgNormalAtlases: TextureAtlases,
) {
    fun spawnBgNormal(
        mapObjectsLayer: MapLayer,
        layerZIndex: Int,
    ) {
        mapObjectsLayer.objects.forEach { normalBg ->

            val textureName = normalBg.properties.get("texture") as? String ?: ""
            val position = vec2(normalBg.x * UNIT_SCALE, normalBg.y * UNIT_SCALE)
            val width = normalBg.width * UNIT_SCALE
            val height = normalBg.height * UNIT_SCALE
            val objZIndex = normalBg.properties.get("zIndex") as? Int ?: 0
            val scale = normalBg.properties.get("scale") as? Float ?: 1f
            world.entity {
                val imgCmp = ImageComponent(stage, zIndex = layerZIndex + objZIndex)
                imgCmp.image = Image()
                it += imgCmp

                val shaderCmp = ShaderRenderingComponent()
                shaderCmp.diffuse = bgNormalAtlases.diffuseAtlas.findRegion(textureName)
                shaderCmp.normal = bgNormalAtlases.normalAtlas?.findRegion(textureName)
                shaderCmp.specular = bgNormalAtlases.specularAtlas?.findRegion(textureName)
                it += shaderCmp
                it += TransformComponent(position, width, height)
                it += TiledTextureComponent(scale)
            }
        }
    }
}
