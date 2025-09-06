@file:Suppress("ktlint:standard:filename")

package io.bennyoe.utility

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.scenes.scene2d.Stage

/**
 * Returns the current viewport dimensions of the given [stage] as [ViewportDimensions].
 */
fun getViewportDimensions(stage: Stage): ViewportDimensions {
    val camera = stage.camera as OrthographicCamera

    val halfW = camera.viewportWidth * camera.zoom / 2f
    val halfH = camera.viewportHeight * camera.zoom / 2f

    return ViewportDimensions(
        camera.position.x - halfW,
        camera.position.y + halfH,
        camera.position.x + halfW,
        camera.position.y - halfH,
        camera.viewportWidth * camera.zoom,
        camera.viewportHeight * camera.zoom,
    )
}

data class ViewportDimensions(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val width: Float,
    val height: Float,
)
