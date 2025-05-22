package io.bennyoe.service

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Circle
import com.badlogic.gdx.math.Ellipse
import com.badlogic.gdx.math.Polygon
import com.badlogic.gdx.math.Polyline
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Shape2D
import ktx.collections.GdxArray
import ktx.collections.gdxArrayOf

class DebugRenderService {
    val shapes: GdxArray<DebugShape> = gdxArrayOf()
}

data class DebugShape(
    val shape: Shape2D,
    val color: Color,
    val label: String = "",
    val type: ShapeRenderer.ShapeType = ShapeRenderer.ShapeType.Line,
    val alpha: Float = 1f,
    var ttl: Float? = null,
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
) {
    service.shapes.add(DebugShape(this, color, label, type, alpha, ttl))
}

fun Circle.addToDebugView(
    service: DebugRenderService,
    color: Color = Color.RED,
    label: String = "",
    type: ShapeRenderer.ShapeType = ShapeRenderer.ShapeType.Line,
    alpha: Float = 1f,
    ttl: Float? = null,
) {
    service.shapes.add(DebugShape(this, color, label, type, alpha, ttl))
}

fun Ellipse.addToDebugView(
    service: DebugRenderService,
    color: Color = Color.RED,
    label: String = "",
    type: ShapeRenderer.ShapeType = ShapeRenderer.ShapeType.Line,
    alpha: Float = 1f,
    ttl: Float? = null,
) {
    service.shapes.add(DebugShape(this, color, label, type, alpha, ttl))
}

fun Polyline.addToDebugView(
    service: DebugRenderService,
    color: Color = Color.RED,
    label: String = "",
    type: ShapeRenderer.ShapeType = ShapeRenderer.ShapeType.Line,
    alpha: Float = 1f,
    ttl: Float? = null,
) {
    service.shapes.add(DebugShape(this, color, label, type, alpha, ttl))
}

fun Polygon.addToDebugView(
    service: DebugRenderService,
    color: Color = Color.RED,
    label: String = "",
    type: ShapeRenderer.ShapeType = ShapeRenderer.ShapeType.Line,
    alpha: Float = 1f,
    ttl: Float? = null,
) {
    service.shapes.add(DebugShape(this, color, label, type, alpha, ttl))
}
