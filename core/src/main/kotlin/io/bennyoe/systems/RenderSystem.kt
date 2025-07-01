package io.bennyoe.systems

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.maps.MapLayer
import com.badlogic.gdx.maps.tiled.TiledMapImageLayer
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer
import com.badlogic.gdx.maps.tiled.objects.TiledMapTileMapObject
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer
import com.badlogic.gdx.maps.tiled.tiles.AnimatedTiledMapTile
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.bennyOe.core.Scene2dLightEngine
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import com.github.quillraven.fleks.World.Companion.inject
import io.bennyoe.components.ImageComponent
import io.bennyoe.config.GameConstants.SHOW_ONLY_DEBUG
import io.bennyoe.config.GameConstants.UNIT_SCALE
import io.bennyoe.event.MapChangedEvent
import ktx.graphics.use
import ktx.log.logger
import ktx.tiled.forEachLayer
import ktx.tiled.layer

class RenderSystem(
    private val stage: Stage = inject("stage"),
    private val lightEngine: Scene2dLightEngine = inject("lightEngine"),
) : IteratingSystem(family { all(ImageComponent) }, enabled = !SHOW_ONLY_DEBUG),
    EventListener {
    private val mapRenderer = OrthogonalTiledMapRenderer(null, UNIT_SCALE, stage.batch)
    private val mapTileLayer: MutableList<TiledMapTileLayer> = mutableListOf()
    private var mapObjectsLayer: MapLayer = MapLayer()
    private val mapBg: MutableList<TiledMapImageLayer> = mutableListOf()
    private val orthoCam = stage.camera as OrthographicCamera

    override fun onTick() {
        // 1. execute logic
        orthoCam.update()
        stage.viewport.apply()
        stage.act(deltaTime)
        lightEngine.update()

        // 2. draw the map
        mapRenderer.setView(orthoCam)
        mapRenderer.render()
        renderMap()

        // 3. call the lightEngine
        lightEngine.renderLights { engine ->
            for (actor: Actor in stage.actors) {
                engine.draw(actor)
            }
        }
        // 4. call super to execute onTickEntity for correct size and transformation
        super.onTick()
    }

    override fun onTickEntity(entity: Entity) {
        val imageCmp = entity[ImageComponent]
        val originalOrFlippedImage = if (imageCmp.flipImage) -imageCmp.scaleX else imageCmp.scaleX
        imageCmp.image.setSize(originalOrFlippedImage, imageCmp.scaleY)
    }

    override fun handle(event: Event): Boolean {
        when (event) {
            is MapChangedEvent -> {
                mapTileLayer.clear()
                mapBg.clear()
                mapRenderer.map = event.map

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
