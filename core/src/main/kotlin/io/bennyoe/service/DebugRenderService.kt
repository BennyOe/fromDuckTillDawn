package io.bennyoe.service

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Circle
import com.badlogic.gdx.math.Ellipse
import com.badlogic.gdx.math.Polygon
import com.badlogic.gdx.math.Polyline
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Shape2D
import io.bennyoe.config.GameConstants.DEBUG_ALPHA
import io.bennyoe.systems.debug.DebugType
import ktx.collections.GdxArray
import ktx.collections.gdxArrayOf

interface DebugRenderService {
    fun addShape(shape: DebugShape)
}

class DefaultDebugRenderService : DebugRenderService {
    val shapes: GdxArray<DebugShape> = gdxArrayOf()

    override fun addShape(shape: DebugShape) {
        shapes.add(shape)
    }
}

class NoOpDebugRenderService : DebugRenderService {
    override fun addShape(shape: DebugShape) = Unit
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
    service: DebugRenderService,
    color: Color = Color.RED,
    label: String = "",
    type: ShapeRenderer.ShapeType = ShapeRenderer.ShapeType.Line,
    alpha: Float = 1f,
    ttl: Float? = null,
    debugType: DebugType = DebugType.NONE,
) {
    service.addShape(DebugShape(this, color, label, type, alpha, ttl, debugType))
}

fun Circle.addToDebugView(
    service: DebugRenderService,
    color: Color = Color.RED,
    label: String = "",
    type: ShapeRenderer.ShapeType = ShapeRenderer.ShapeType.Line,
    alpha: Float = 1f,
    ttl: Float? = null,
    debugType: DebugType = DebugType.NONE,
) {
    service.addShape(DebugShape(this, color, label, type, alpha, ttl, debugType))
}

fun Ellipse.addToDebugView(
    service: DebugRenderService,
    color: Color = Color.RED,
    label: String = "",
    type: ShapeRenderer.ShapeType = ShapeRenderer.ShapeType.Line,
    alpha: Float = 1f,
    ttl: Float? = null,
    debugType: DebugType = DebugType.NONE,
) {
    service.addShape(DebugShape(this, color, label, type, alpha, ttl, debugType))
}

fun Polyline.addToDebugView(
    service: DebugRenderService,
    color: Color = Color.RED,
    label: String = "",
    type: ShapeRenderer.ShapeType = ShapeRenderer.ShapeType.Line,
    alpha: Float = 1f,
    ttl: Float? = null,
    debugType: DebugType = DebugType.NONE,
) {
    service.addShape(DebugShape(this, color, label, type, alpha, ttl, debugType))
}

fun Polygon.addToDebugView(
    service: DebugRenderService,
    color: Color = Color.RED,
    label: String = "",
    type: ShapeRenderer.ShapeType = ShapeRenderer.ShapeType.Line,
    alpha: Float = 1f,
    ttl: Float? = null,
    debugType: DebugType = DebugType.NONE,
) {
    service.addShape(DebugShape(this, color, label, type, alpha, ttl, debugType))
}
