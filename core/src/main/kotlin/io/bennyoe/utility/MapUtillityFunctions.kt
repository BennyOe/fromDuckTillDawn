package io.bennyoe.utility

import com.badlogic.gdx.maps.MapGroupLayer
import com.badlogic.gdx.maps.MapLayer
import com.badlogic.gdx.maps.MapLayers

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
