package io.bennyoe.systems

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.maps.tiled.TiledMapImageLayer
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer
import com.badlogic.gdx.maps.tiled.tiles.AnimatedTiledMapTile
import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.quillraven.fleks.IntervalSystem
import com.github.quillraven.fleks.World.Companion.inject
import io.bennyoe.config.GameConstants.UNIT_SCALE
import io.bennyoe.event.MapChangedEvent
import ktx.graphics.use
import ktx.tiled.forEachLayer

class RenderMapSystem(
    private val stage: Stage = inject("stage"),
) : IntervalSystem(),
    EventListener {
    private val mapRenderer = OrthogonalTiledMapRenderer(null, UNIT_SCALE, stage.batch)
    private val mapTileLayer: MutableList<TiledMapTileLayer> = mutableListOf()
    private val mapBg: MutableList<TiledMapImageLayer> = mutableListOf()
    private val orthoCam = stage.camera as OrthographicCamera

    override fun onTick() {
        stage.viewport.apply()
        mapRenderer.setView(orthoCam)
        mapRenderer.render()
        renderMap()
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
        }
    }
}
