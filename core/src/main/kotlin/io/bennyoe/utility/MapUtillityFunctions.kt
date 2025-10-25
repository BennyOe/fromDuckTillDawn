package io.bennyoe.utility

import com.badlogic.gdx.maps.MapGroupLayer
import com.badlogic.gdx.maps.MapLayer
import com.badlogic.gdx.maps.MapLayers
import com.badlogic.gdx.maps.tiled.TiledMapImageLayer
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer

// finds layers in layer-groups
fun MapLayers.findLayerDeep(name: String): MapLayer? {
    for (layer in this) {
        if (layer.name == name) return layer
        if (layer is MapGroupLayer) {
            val subLayers = layer.layers
            val result = subLayers.findLayerDeep(name)
            if (result != null) return result
        }
    }
    return null
}

// returns all ImageLayers from a map (also in layer-groups)
fun MapLayers.findImageLayerDeep(): List<TiledMapImageLayer> {
    val result = mutableListOf<TiledMapImageLayer>()

    for (layer in this) {
        when (layer) {
            is TiledMapImageLayer -> result.add(layer)
            is MapGroupLayer -> result.addAll(layer.layers.findImageLayerDeep())
        }
    }
    return result
}

// returns all TileLayers from a map (also in layer-groups)
fun MapLayers.findTileLayerDeep(): List<TiledMapTileLayer> {
    val result = mutableListOf<TiledMapTileLayer>()

    for (layer in this) {
        when (layer) {
            is TiledMapTileLayer -> result.add(layer)
            is MapGroupLayer -> result.addAll(layer.layers.findTileLayerDeep())
        }
    }
    return result
}

/**
 * Finds all layers recursively within the given layers collection
 * whose names start with the specified prefix.
 */
fun MapLayers.findLayersStartingWithDeep(prefix: String): List<MapLayer> {
    val result = mutableListOf<MapLayer>()
    this.forEach { layer ->
        if (layer.name.startsWith(prefix)) {
            result.add(layer)
        }
        if (layer is MapGroupLayer) {
            result.addAll(layer.layers.findLayersStartingWithDeep(prefix)) // Recursive call
        }
    }
    return result
}
