package io.bennyoe.systems.debug

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Circle
import com.badlogic.gdx.math.Ellipse
import com.badlogic.gdx.math.Polygon
import com.badlogic.gdx.math.Polyline
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer
import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IntervalSystem
import com.github.quillraven.fleks.World.Companion.inject
import io.bennyoe.components.StateComponent
import io.bennyoe.components.UiComponent
import io.bennyoe.components.ai.BehaviorTreeComponent
import io.bennyoe.components.debug.BTBubbleComponent
import io.bennyoe.components.debug.DebugComponent
import io.bennyoe.components.debug.StateBubbleComponent
import io.bennyoe.config.GameConstants.ENABLE_DEBUG
import io.bennyoe.ui.GameView
import io.bennyoe.ui.widgets.debug.LabelWidget
import ktx.assets.disposeSafely
import ktx.graphics.use
import ktx.log.logger
import com.badlogic.gdx.physics.box2d.World as PhyWorld

class DebugSystem(
    private val phyWorld: PhyWorld =
        inject("phyWorld"),
    private val stage: Stage =
        inject("stage"),
    private val uiStage: Stage =
        inject("uiStage"),
    private val debugRenderingService: DefaultDebugRenderService =
        inject("debugRenderService"),
    val shapeRenderer: ShapeRenderer =
        inject("shapeRenderer"),
) : IntervalSystem(enabled = ENABLE_DEBUG) {
    private val tmpVec = Vector3()
    private val physicsRenderer by lazy { Box2DDebugRenderer() }
    private val labels = hashMapOf<DebugShape, LabelWidget>()
    private val gameView: GameView by lazy {
        uiStage.actors.filterIsInstance<GameView>().first()
    }
    private var lastDebugState = false
    private val debugEntity by lazy { world.family { all(DebugComponent) }.firstOrNull() }
    private val debugCmp by lazy { debugEntity?.let { entity -> entity[DebugComponent] } }
    private val debugCfg =
        mutableMapOf(
            DebugType.NONE to true,
            DebugType.PLAYER to false,
            DebugType.ENEMY to false,
            DebugType.ATTACK to false,
            DebugType.CAMERA to false,
        )

    override fun onTick() {
        val playerEntity = world.family { all(StateComponent) }.firstOrNull() ?: return
        val enemyEntities = world.family { all(BehaviorTreeComponent) }
        debugCmp?.let { debugComponent ->
            debugCfg[DebugType.PLAYER] = debugComponent.playerDebugEnabled
            debugCfg[DebugType.ENEMY] = debugComponent.enemyDebugEnabled
            debugCfg[DebugType.ATTACK] = debugComponent.attackDebugEnabled
            debugCfg[DebugType.CAMERA] = debugComponent.cameraDebugEnabled

            if (debugComponent.enabled != lastDebugState) {
                gameView.toggleDebugOverlay()
                lastDebugState = debugComponent.enabled
            }

            if (debugComponent.enabled) {
                stage.viewport.apply()
                physicsRenderer.isDrawVelocities = debugComponent.drawVelocities
                physicsRenderer.isDrawBodies = debugComponent.drawPhysicBodies
                physicsRenderer.render(phyWorld, stage.camera.combined)

                enemyEntities.forEach { enemyEntity ->
                    addStateBubbles(enemyEntity, playerEntity)
                    addBTBubbles(enemyEntity)
                }

                val visibleShapes = debugRenderingService.shapes.filter { shape -> debugCfg[shape.debugType] == true }
                drawDebugLines(visibleShapes)
                purgeStaleLabels(visibleShapes.toSet())
            } else {
                enemyEntities.forEach { enemyEntity ->
                    removeStateBubbles(playerEntity, enemyEntity)
                    removeBTBubbles(enemyEntity)
                }

                uiStage.actors.removeAll { it is LabelWidget }
                labels.values.forEach { it.remove() }
                labels.clear()
            }
        }

        // clear the shapes to avoid increasing draw calls per frame
        clearDebugShapes()
    }

    private fun addBTBubbles(enemyEntity: Entity) {
        when {
            enemyEntity hasNo BTBubbleComponent -> world.entity { enemyEntity += BTBubbleComponent(uiStage) }
            enemyEntity hasNo UiComponent -> world.entity { enemyEntity += UiComponent }
        }
    }

    private fun removeBTBubbles(enemyEntity: Entity) {
        when {
            enemyEntity has BTBubbleComponent -> enemyEntity.configure { it -= BTBubbleComponent }
            enemyEntity has UiComponent -> enemyEntity.configure { it -= UiComponent }
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

    /*
    The `drawDebugLines` method is responsible for rendering debug shapes and labels on the screen.
    It uses a `ShapeRenderer` to draw shapes like rectangles and circles in the world coordinate system and overlays labels using a `SpriteBatch`
    for text rendering. The method iterates over a collection of debug shapes provided by the `DebugRenderService`, applies the appropriate
    projection matrices for world and UI rendering, and ensures proper alignment of shapes and labels in both coordinate systems.
     */
    private fun drawDebugLines(visibleShapes: List<DebugShape>) {
        visibleShapes.groupBy { it.shapeType }.forEach { (type, shapes) ->
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
                            drawLabel(dbgShape.shape.vertices[dbgShape.shape.vertices.size - 2], dbgShape.shape.vertices.last(), dbgShape)
                        }

                        is Polygon -> {
                            val v = dbgShape.shape.vertices
                            if (type == ShapeRenderer.ShapeType.Filled && v.size == 6) {
                                // only triangles can be drawn with ShapeType.filled
                                it.triangle(
                                    v[0],
                                    v[1],
                                    v[2],
                                    v[3],
                                    v[4],
                                    v[5],
                                )
                            } else {
                                it.polygon(v)
                            }
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
        tmpVec.set(x, y, 0f)
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
