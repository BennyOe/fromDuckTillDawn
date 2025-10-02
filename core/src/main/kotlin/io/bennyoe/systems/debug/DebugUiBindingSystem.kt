package io.bennyoe.systems.debug

import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.quillraven.fleks.IntervalSystem
import com.github.quillraven.fleks.World.Companion.inject
import io.bennyoe.components.GameStateComponent
import io.bennyoe.components.debug.DebugComponent
import io.bennyoe.ui.GameView
import kotlin.math.roundToInt

class DebugUiBindingSystem(
    private val uiStage: Stage = inject("uiStage"),
    private val debugRenderingService: DefaultDebugRenderService =
        inject("debugRenderService"),
) : IntervalSystem() {
    private val debugFamily = world.family { all(DebugComponent) }
    private val gameStateFamily = world.family { all(GameStateComponent) }

    // Find the GameView once, as it's a persistent UI element.
    private val gameView: GameView? by lazy { uiStage.actors.filterIsInstance<GameView>().firstOrNull() }

    override fun onTick() {
        val gv = gameView ?: return
        val debugWidget = gv.debugWidget
        val debugEntity = debugFamily.firstOrNull() ?: return
        val gameStateEntity = gameStateFamily.firstOrNull() ?: return

        val debugCmp = debugEntity[DebugComponent]
        val gameStateCmp = gameStateEntity[GameStateComponent]

        // --- 1. UI -> Game State ---
        // Read values from widgets and write them to the components.
        debugCmp.debugTimeScale = debugWidget.gameSpeedSlider.value

        gameStateCmp.musicVolume = debugWidget.musicVolumeSlider.value
        gameStateCmp.ambienceVolume = debugWidget.ambienceVolumeSlider.value
        gameStateCmp.sfxVolume = debugWidget.sfxVolumeSlider.value

        debugCmp.drawPhysicBodies = debugWidget.physicBodiesDebugCheckBox.isChecked
        debugCmp.drawVelocities = debugWidget.velocityDebugCheckBox.isChecked
        debugCmp.playerDebugEnabled = debugWidget.playerDebugCheckBox.isChecked
        debugCmp.enemyDebugEnabled = debugWidget.enemyDebugCheckBox.isChecked
        debugCmp.attackDebugEnabled = debugWidget.attackDebugCheckBox.isChecked
        debugCmp.cameraDebugEnabled = debugWidget.cameraDebugCheckBox.isChecked

        // --- 2. Game State -> UI ---
        // Read values from components and update the UI.
        // This ensures the UI reflects the state even if it's changed programmatically.
        val speedPercent = (debugCmp.debugTimeScale * 100).roundToInt()
        debugWidget.gameSpeedLabel.setText("Game speed: $speedPercent%")

        val musicPercent = (gameStateCmp.musicVolume * 100).roundToInt()
        val ambiencePercent = (gameStateCmp.ambienceVolume * 100).roundToInt()
        val sfxPercent = (gameStateCmp.sfxVolume * 100).roundToInt()
        debugWidget.musicVolumeLabel.setText("Music: $musicPercent%")
        debugWidget.ambienceVolumeLabel.setText("Ambience: $ambiencePercent%")
        debugWidget.sfxVolumeLabel.setText("Sfx: $sfxPercent%")

        // Also set slider and checkboxes in case the state was changed from code
        debugWidget.gameSpeedSlider.value = debugCmp.debugTimeScale
        debugWidget.playerDebugCheckBox.isChecked = debugCmp.playerDebugEnabled
        debugWidget.enemyDebugCheckBox.isChecked = debugCmp.enemyDebugEnabled
        debugWidget.attackDebugCheckBox.isChecked = debugCmp.attackDebugEnabled
        debugWidget.cameraDebugCheckBox.isChecked = debugCmp.cameraDebugEnabled

        DebugPropsManager.flush()
        debugWidget.setDebugProperties(debugRenderingService.renderToDebugProperties)
    }
}
