package io.bennyoe.systems.debug

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.graphics.profiling.GLProfiler
import com.badlogic.gdx.math.Circle
import com.badlogic.gdx.math.Ellipse
import com.badlogic.gdx.math.Polygon
import com.badlogic.gdx.math.Polyline
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IntervalSystem
import com.github.quillraven.fleks.World.Companion.inject
import io.bennyoe.components.AiComponent
import io.bennyoe.components.ImageComponent
import io.bennyoe.components.PhysicComponent
import io.bennyoe.components.StateComponent
import io.bennyoe.components.UiComponent
import io.bennyoe.components.debug.DebugComponent
import io.bennyoe.components.debug.StateBubbleComponent
import io.bennyoe.config.GameConstants.SHOW_ATTACK_DEBUG
import io.bennyoe.config.GameConstants.SHOW_CAMERA_DEBUG
import io.bennyoe.config.GameConstants.SHOW_ENEMY_DEBUG
import io.bennyoe.config.GameConstants.SHOW_PLAYER_DEBUG
import io.bennyoe.service.DebugRenderService
import io.bennyoe.service.DebugShape
import io.bennyoe.service.addToDebugView
import io.bennyoe.widgets.DrawCallsCounterWidget
import io.bennyoe.widgets.FpsCounterWidget
import io.bennyoe.widgets.LabelWidget
import ktx.assets.disposeSafely
import ktx.graphics.use
import ktx.log.logger
import ktx.math.vec2
import com.badlogic.gdx.physics.box2d.World as PhyWorld

class DebugSystem(
    private val phyWorld: PhyWorld =
        inject("phyWorld"),
    private val stage: Stage =
        inject("stage"),
    private val uiStage: Stage =
        inject("uiStage"),
    private val debugRenderingService: DebugRenderService =
        inject("debugRenderService"),
    val shapeRenderer: ShapeRenderer =
        inject("shapeRenderer"),
    profiler: GLProfiler =
        inject("profiler"),
) : IntervalSystem(enabled = true) {
    private val physicsRenderer by lazy { Box2DDebugRenderer() }
    private val fpsLabelStyle = LabelStyle(BitmapFont().apply { data.setScale(1.5f) }, Color(0f, 1f, 0f, 1f))
    private val labels = hashMapOf<DebugShape, LabelWidget>()
    private val debugCfg =
        mapOf(
            DebugType.ATTACK to SHOW_ATTACK_DEBUG,
            DebugType.PLAYER to SHOW_PLAYER_DEBUG,
            DebugType.CAMERA to SHOW_CAMERA_DEBUG,
            DebugType.ENEMY to SHOW_ENEMY_DEBUG,
            DebugType.NONE to true,
        )
    private val fpsCounter =
        FpsCounterWidget(fpsLabelStyle).apply {
            setPosition(10f, 20f)
        }
    private val drawCallsCounter =
        DrawCallsCounterWidget(fpsLabelStyle, profiler).apply {
            setPosition(Gdx.graphics.width - 140f, 20f)
        }

    init {
        uiStage.addActor(fpsCounter)
        uiStage.addActor(drawCallsCounter)
    }

    override fun onTick() {
        val debugEntity = world.family { all(DebugComponent) }.firstOrNull() ?: return
        val debugCmp = debugEntity.let { entity -> entity[DebugComponent] }

        val playerEntity = world.family { all(StateComponent) }.firstOrNull() ?: return
        val enemyEntity = world.family { all(AiComponent) }.firstOrNull() ?: return

        // TODO just experimental use of rays ... REFACTOR in own system
//        spawnRays(playerEntity)

        fpsCounter.isVisible = debugCmp.enabled
        drawCallsCounter.isVisible = debugCmp.enabled
        if (debugCmp.enabled) {
            addStateBubbles(enemyEntity, playerEntity)
            fpsCounter.act(deltaTime)
            drawCallsCounter.act(deltaTime)
            physicsRenderer.render(phyWorld, stage.camera.combined)
            val currentShapes = debugRenderingService.shapes.toSet()
            drawDebugLines()
            purgeStaleLabels(currentShapes)
        } else {
            uiStage.actors.removeAll { it is LabelWidget }
            removeStateBubbles(playerEntity, enemyEntity)
            labels.values.forEach { it.remove() }
            labels.clear()
        }
    }

    private fun removeStateBubbles(
        playerEntity: Entity,
        enemyEntity: Entity,
    ) {
        when {
            enemyEntity has StateBubbleComponent -> enemyEntity.configure { it -= StateBubbleComponent }

            enemyEntity has UiComponent -> enemyEntity.configure { it -= UiComponent }

            playerEntity has StateBubbleComponent -> playerEntity.configure { it -= StateBubbleComponent }

            playerEntity has UiComponent -> playerEntity.configure { it -= UiComponent }
        }
    }

    private fun addStateBubbles(
        enemyEntity: Entity,
        playerEntity: Entity,
    ) {
        when {
            enemyEntity hasNo StateBubbleComponent -> world.entity { enemyEntity += StateBubbleComponent(uiStage) }

            enemyEntity hasNo UiComponent -> world.entity { enemyEntity += UiComponent }

            playerEntity hasNo StateBubbleComponent -> world.entity { playerEntity += StateBubbleComponent(uiStage) }

            playerEntity hasNo UiComponent -> world.entity { playerEntity += UiComponent }
        }
    }

    private fun spawnRays(playerEntity: Entity) {
        val phyCmp = playerEntity[PhysicComponent]
        val imageCmp = playerEntity[ImageComponent]
        for (range in -20..20 step 4) {
            val rangeInFloat = range.toFloat() / 10
            val rayLength = 3
            val rayStart = phyCmp.body.position
            val rayEnd =
                if (imageCmp.flipImage) {
                    vec2(rayStart.x - rayLength, rayStart.y - rangeInFloat)
                } else {
                    vec2(rayStart.x + rayLength, rayStart.y + rangeInFloat)
                }

            phyWorld.rayCast({ fixture, point, normal, fraction ->
                logger.debug { "Hit fixture ${fixture.body.userData}" }
                1f
            }, rayStart, rayEnd)
            Polyline(floatArrayOf(rayStart.x, rayStart.y, rayEnd.x, rayEnd.y)).addToDebugView(
                debugRenderingService,
                Color.CHARTREUSE,
                debugType =
                    DebugType.PLAYER,
            )
        }
    }

    /*
    The `drawDebugLines` method is responsible for rendering debug shapes and labels on the screen.
    It uses a `ShapeRenderer` to draw shapes like rectangles and circles in the world coordinate system and overlays labels using a `SpriteBatch`
    for text rendering. The method iterates over a collection of debug shapes provided by the `DebugRenderService`, applies the appropriate
    projection matrices for world and UI rendering, and ensures proper alignment of shapes and labels in both coordinate systems.
     */
    private fun drawDebugLines() {
        debugRenderingService.shapes.filter { debugCfg[it.debugType] == true }.groupBy { it.shapeType }.forEach { (type, shapes) ->
            shapeRenderer.use(type) {
                // this needs to be set to allow transparency alpha
                Gdx.gl.glEnable(GL20.GL_BLEND)
                Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)

                // set the shapeRenderer to WorldUnits
                it.projectionMatrix = stage.camera.combined
                // draw WorldUnit stuff here
                shapes.forEach { dbgShape ->
                    // fade out of shape
                    dbgShape.alpha = (if (dbgShape.ttl != null && dbgShape.ttl!! < 1f) dbgShape.ttl else dbgShape.alpha)!!

                    it.color = Color(dbgShape.color.r, dbgShape.color.g, dbgShape.color.b, dbgShape.alpha)
                    when (dbgShape.shape) {
                        is Rectangle -> {
                            it.rect(
                                dbgShape.shape.x,
                                dbgShape.shape.y,
                                dbgShape.shape.width,
                                dbgShape.shape.height,
                            )
                            drawLabel(dbgShape.shape.x, dbgShape.shape.y, dbgShape)
                        }

                        is Circle -> {
                            it.arc(dbgShape.shape.x, dbgShape.shape.y, dbgShape.shape.radius, 0f, 360f, 30)
                            drawLabel(dbgShape.shape.x, dbgShape.shape.y, dbgShape)
                        }

                        is Ellipse -> {
                            it.ellipse(dbgShape.shape.x, dbgShape.shape.y, dbgShape.shape.width, dbgShape.shape.height)
                            drawLabel(dbgShape.shape.x, dbgShape.shape.y, dbgShape)
                        }

                        is Polyline -> {
                            it.polyline(dbgShape.shape.vertices)
                            drawLabel(dbgShape.shape.vertices[0], dbgShape.shape.vertices[1], dbgShape)
                        }

                        is Polygon -> {
                            it.polygon(dbgShape.shape.vertices)
                            drawLabel(dbgShape.shape.vertices[0], dbgShape.shape.vertices[1], dbgShape)
                        }
                    }
                }
            }
        }

        shapeRenderer.use(ShapeRenderer.ShapeType.Line) {
            // set the shapeRenderer to Pixels
            it.projectionMatrix = uiStage.camera.combined
            // draw pixel stuff here
        }

        // clear the shapes to avoid increasing draw calls per frame
        clearDebugShapes()
    }

    private fun clearDebugShapes() {
        debugRenderingService.shapes.forEach { shape ->
            if (shape.ttl == null || shape.ttl!! <= 0f) {
                debugRenderingService.shapes.removeValue(shape, false)
            } else {
                shape.ttl = shape.ttl!! - deltaTime
            }
        }
    }

    // this renders the label. Therefore, there must be used pixels instead of WU
    private fun drawLabel(
        x: Float,
        y: Float,
        dbgShape: DebugShape,
    ) {
        val tmpVec = Vector3(x, y, 0f)
        // converts tmpVec worldUnits -> pixel
        stage.viewport.project(tmpVec)
        uiStage.viewport.unproject(tmpVec)

        val label =
            labels.getOrPut(dbgShape) {
                LabelWidget(dbgShape.label, dbgShape.color).also { uiStage.addActor(it) }
            }

        label.setPosition(tmpVec.x, uiStage.height - tmpVec.y)
    }

    private fun purgeStaleLabels(activeShapes: Set<DebugShape>) {
        val itr = labels.iterator()
        while (itr.hasNext()) {
            val (shape, label) = itr.next()
            if (shape !in activeShapes) {
                label.remove()
                itr.remove()
            }
        }
    }

    override fun onDispose() {
        if (enabled) {
            physicsRenderer.disposeSafely()
        }
    }

    companion object {
        private val logger = logger<DebugSystem>()
    }
}
