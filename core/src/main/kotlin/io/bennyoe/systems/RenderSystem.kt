package io.bennyoe.systems

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.maps.MapLayer
import com.badlogic.gdx.maps.tiled.TiledMapImageLayer
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer
import com.badlogic.gdx.maps.tiled.objects.TiledMapTileMapObject
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer
import com.badlogic.gdx.maps.tiled.tiles.AnimatedTiledMapTile
import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import com.github.quillraven.fleks.World.Companion.inject
import io.bennyoe.components.ImageComponent
import io.bennyoe.config.GameConstants.UNIT_SCALE
import io.bennyoe.event.MapChangedEvent
import ktx.graphics.use
import ktx.log.logger
import ktx.tiled.forEachLayer
import ktx.tiled.layer

class RenderSystem(
    private val stage: Stage = inject("stage"),
) : IteratingSystem(family { all(ImageComponent) }, enabled = true),
    EventListener {
    private val mapRenderer = OrthogonalTiledMapRenderer(null, UNIT_SCALE, stage.batch)
    private val mapTileLayer: MutableList<TiledMapTileLayer> = mutableListOf()
    private var mapObjectsLayer: MapLayer = MapLayer()
    private val mapBg: MutableList<TiledMapImageLayer> = mutableListOf()
    private val orthoCam = stage.camera as OrthographicCamera

    override fun onTick() {
        with(stage) {
            viewport.apply()
            renderMap()
            act(deltaTime)
            draw()
        }
        super.onTick()
    }

    override fun onTickEntity(entity: Entity) {
        val imageComponent = entity[ImageComponent]
        val originalOrFlippedImage = if (imageComponent.flipImage) -imageComponent.scaleX else imageComponent.scaleX
        imageComponent.image.setSize(originalOrFlippedImage, imageComponent.scaleY)
    }

    override fun handle(event: Event): Boolean {
        when (event) {
            is MapChangedEvent -> {
                event.map.forEachLayer<TiledMapTileLayer> { layer ->
                    mapTileLayer.add(layer)
                }
                event.map.forEachLayer<TiledMapImageLayer> { layer ->
                    mapBg.add(layer)
                }
                mapObjectsLayer = event.map.layer("fireObject")
            }
        }
        return true
    }

    private fun renderMap() {
        AnimatedTiledMapTile.updateAnimationBaseTime() // is called to render animated tiles in the map
        mapRenderer.setView(orthoCam)
        // this is rendering the map
        stage.batch.use(orthoCam.combined) {
            mapBg.forEach {
                mapRenderer.renderImageLayer(it)
            }
            mapTileLayer.forEach {
                mapRenderer.renderTileLayer(it)
            }
            renderObjects()
        }
    }

    private fun renderObjects() {
        mapObjectsLayer.objects.forEach { mapObject ->
            if (mapObject is TiledMapTileMapObject) {
                val textureRegion = mapObject.tile.textureRegion
                val x = mapObject.x * UNIT_SCALE
                val y = mapObject.y * UNIT_SCALE
                val width = textureRegion.regionWidth.toFloat() * UNIT_SCALE
                val height = textureRegion.regionHeight.toFloat() * UNIT_SCALE

                stage.batch.draw(
                    textureRegion,
                    x,
                    y,
                    width,
                    height,
                )
            }
        }
    }

    companion object {
        val logger = logger<RenderSystem>()
    }
}
