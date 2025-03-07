package io.bennyoe.systems

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.maps.tiled.TiledMapImageLayer
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer
import com.badlogic.gdx.maps.tiled.tiles.AnimatedTiledMapTile
import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import com.github.quillraven.fleks.World.Companion.inject
import io.bennyoe.Duckee.Companion.UNIT_SCALE
import io.bennyoe.components.ImageComponent
import io.bennyoe.event.MapChangedEvent
import ktx.graphics.use
import ktx.tiled.forEachLayer

class RenderSystem(
    private val stage: Stage = inject()
) : IteratingSystem(family { all(ImageComponent) }), EventListener {
    private val mapRenderer = OrthogonalTiledMapRenderer(null, UNIT_SCALE, stage.batch)
    private lateinit var map: TiledMapTileLayer
    private val mapBg: MutableList<TiledMapImageLayer> = mutableListOf()
    private val orthoCam = stage.camera as OrthographicCamera

    override fun onTick() {
        with(stage) {
            viewport.apply()
            AnimatedTiledMapTile.updateAnimationBaseTime() // is called to render animated tiles in the map

            mapRenderer.setView(orthoCam)
            // this is rendering the map
            stage.batch.use(orthoCam.combined) {
                mapBg.forEach {
                    mapRenderer.renderImageLayer(it)
                }
                mapRenderer.renderTileLayer(map)
            }


            camera.update()
            act(deltaTime)
        }
        stage.draw()
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
                    map = layer
                }
                event.map.forEachLayer<TiledMapImageLayer> { layer ->
                    mapBg.add(layer)
                }
            }
        }
        return true
    }
}
