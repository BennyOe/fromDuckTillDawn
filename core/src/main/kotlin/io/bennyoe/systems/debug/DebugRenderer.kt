package io.bennyoe.systems.debug

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Circle
import com.badlogic.gdx.math.Ellipse
import com.badlogic.gdx.math.Polygon
import com.badlogic.gdx.math.Polyline
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Shape2D
import io.bennyoe.config.GameConstants.DEBUG_ALPHA
import io.bennyoe.config.GameConstants.ENABLE_DEBUG
import ktx.collections.GdxArray
import ktx.collections.gdxArrayOf

interface DebugRenderer {
    fun addShape(shape: DebugShape)

    fun addProperty(
        name: String,
        value: Any,
    )
}

class DefaultDebugRenderService : DebugRenderer {
    val shapes: GdxArray<DebugShape> = gdxArrayOf()
    val renderToDebugProperties = mutableMapOf<String, Any>()

    override fun addShape(shape: DebugShape) {
        shapes.add(shape)
    }

    override fun addProperty(
        name: String,
        value: Any,
    ) {
        renderToDebugProperties[name] = value
    }
}

class NoOpDebugRenderService : DebugRenderer {
    override fun addShape(shape: DebugShape) = Unit

    override fun addProperty(
        name: String,
        value: Any,
    ) = Unit
}

data class DebugShape(
    val shape: Shape2D,
    val color: Color,
    val label: String = "",
    val shapeType: ShapeRenderer.ShapeType = ShapeRenderer.ShapeType.Line,
    var alpha: Float = DEBUG_ALPHA,
    var ttl: Float? = null,
    var debugType: DebugType = DebugType.NONE,
) {
    // has to be overwritten because position can change and then the label is rendered multiple times
    override fun equals(other: Any?): Boolean = other is DebugShape && other.label == label

    override fun hashCode(): Int = label.hashCode()
}

fun Rectangle.addToDebugView(
    service: DebugRenderer,
    color: Color = Color.RED,
    label: String = "",
    type: ShapeRenderer.ShapeType = ShapeRenderer.ShapeType.Line,
    alpha: Float = 1f,
    ttl: Float? = null,
    debugType: DebugType = DebugType.NONE,
) {
    if (ENABLE_DEBUG) service.addShape(DebugShape(this, color, label, type, alpha, ttl, debugType))
}

fun Circle.addToDebugView(
    service: DebugRenderer,
    color: Color = Color.RED,
    label: String = "",
    type: ShapeRenderer.ShapeType = ShapeRenderer.ShapeType.Line,
    alpha: Float = 1f,
    ttl: Float? = null,
    debugType: DebugType = DebugType.NONE,
) {
    if (ENABLE_DEBUG) service.addShape(DebugShape(this, color, label, type, alpha, ttl, debugType))
}

fun Ellipse.addToDebugView(
    service: DebugRenderer,
    color: Color = Color.RED,
    label: String = "",
    type: ShapeRenderer.ShapeType = ShapeRenderer.ShapeType.Line,
    alpha: Float = 1f,
    ttl: Float? = null,
    debugType: DebugType = DebugType.NONE,
) {
    if (ENABLE_DEBUG) service.addShape(DebugShape(this, color, label, type, alpha, ttl, debugType))
}

fun Polyline.addToDebugView(
    service: DebugRenderer,
    color: Color = Color.RED,
    label: String = "",
    type: ShapeRenderer.ShapeType = ShapeRenderer.ShapeType.Line,
    alpha: Float = 1f,
    ttl: Float? = null,
    debugType: DebugType = DebugType.NONE,
) {
    if (ENABLE_DEBUG) service.addShape(DebugShape(this, color, label, type, alpha, ttl, debugType))
}

fun Polygon.addToDebugView(
    service: DebugRenderer,
    color: Color = Color.RED,
    label: String = "",
    type: ShapeRenderer.ShapeType = ShapeRenderer.ShapeType.Line,
    alpha: Float = 1f,
    ttl: Float? = null,
    debugType: DebugType = DebugType.NONE,
) {
    if (ENABLE_DEBUG) service.addShape(DebugShape(this, color, label, type, alpha, ttl, debugType))
}
