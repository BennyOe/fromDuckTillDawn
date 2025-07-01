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
        // 1. Logik ausführen
        orthoCam.update()
        stage.viewport.apply()
        stage.act(deltaTime)
        lightEngine.update()

        // 2. Die Karte im Hintergrund zeichnen
        mapRenderer.setView(orthoCam)
        mapRenderer.render()

        // 3. Rufe die LightEngine auf, um die Stage zu zeichnen
        lightEngine.renderLights { engine ->
            for (actor: Actor in stage.actors) {
                engine.draw(actor)
            }
        }
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
                mapRenderer.map = event.map // Wichtig: Dem Renderer die neue Karte mitteilen

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

    private fun renderMapBackground() {
        AnimatedTiledMapTile.updateAnimationBaseTime()
        mapRenderer.setView(orthoCam)

        // Kein "batch.use" hier!
        mapBg.forEach {
            mapRenderer.renderImageLayer(it)
        }
        mapTileLayer.forEach {
            mapRenderer.renderTileLayer(it)
        }
    }

    private fun renderMapForeground() {
        // Kein "batch.use" hier!
        renderObjects()
    }

    private fun renderObjects() {
        // Der Batch wird bereits von der LightEngine verwaltet
        val batch = mapRenderer.batch
        mapObjectsLayer.objects.forEach { mapObject ->
            if (mapObject is TiledMapTileMapObject) {
                batch.draw(
                    mapObject.tile.textureRegion,
                    mapObject.x * UNIT_SCALE,
                    mapObject.y * UNIT_SCALE,
                    mapObject.tile.textureRegion.regionWidth * UNIT_SCALE,
                    mapObject.tile.textureRegion.regionHeight * UNIT_SCALE
                )
            }
        }
    }

    companion object {
        val logger = logger<RenderSystem>()
    }
}
