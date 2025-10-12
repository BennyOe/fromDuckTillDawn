package io.bennyoe.systems.entitySpawn

import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.maps.MapLayer
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.github.quillraven.fleks.World
import io.bennyoe.components.ImageComponent
import io.bennyoe.components.IsForeground
import io.bennyoe.components.TransformComponent
import io.bennyoe.config.GameConstants.UNIT_SCALE
import ktx.math.vec2
import ktx.tiled.height
import ktx.tiled.width
import ktx.tiled.x
import ktx.tiled.y

class ForegroundSpawner(
    private val world: World,
    private val stage: Stage,
    private val foregroundAtlas: TextureAtlas,
) {
    fun spawnForeground(
        mapObjectsLayer: MapLayer,
        layerZIndex: Int,
    ) {
        mapObjectsLayer.objects.forEach { foreground ->

            val textureName = foreground.properties.get("texture") as? String ?: ""
            val position = vec2(foreground.x * UNIT_SCALE, foreground.y * UNIT_SCALE)
            val width = foreground.width * UNIT_SCALE
            val height = foreground.height * UNIT_SCALE
            val objZIndex = foreground.properties.get("zIndex") as? Int ?: 0
            world.entity {
                val imgCmp = ImageComponent(stage, zIndex = layerZIndex + objZIndex)
                imgCmp.image = Image(foregroundAtlas.findRegion(textureName))
                it += imgCmp

                it += TransformComponent(position, width, height)
                it += IsForeground
            }
        }
    }
}
