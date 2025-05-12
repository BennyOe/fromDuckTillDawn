package io.bennyoe.service

import com.badlogic.gdx.graphics.Color
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
) {
    // has to be overwritten because position can change and then the label is rendered multiple times
    override fun equals(other: Any?): Boolean = other is DebugShape && other.label == label

    override fun hashCode(): Int = label.hashCode()
}

fun Rectangle.addToDebugView(
    service: DebugRenderService,
    color: Color = Color.RED,
    label: String = "",
) {
    service.shapes.add(DebugShape(this, color, label))
}

fun Circle.addToDebugView(
    service: DebugRenderService,
    color: Color = Color.RED,
    label: String = "",
) {
    service.shapes.add(DebugShape(this, color, label))
}

fun Ellipse.addToDebugView(
    service: DebugRenderService,
    color: Color = Color.RED,
    label: String = "",
) {
    service.shapes.add(DebugShape(this, color, label))
}

fun Polyline.addToDebugView(
    service: DebugRenderService,
    color: Color = Color.RED,
    label: String = "",
) {
    service.shapes.add(DebugShape(this, color, label))
}

fun Polygon.addToDebugView(
    service: DebugRenderService,
    color: Color = Color.RED,
    label: String = "",
) {
    service.shapes.add(DebugShape(this, color, label))
}
