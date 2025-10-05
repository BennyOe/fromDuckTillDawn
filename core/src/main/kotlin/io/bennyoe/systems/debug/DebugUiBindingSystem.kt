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
    private val gameView: GameView? by lazy { uiStage.actors.filterIsInstance<GameView>().firstOrNull() }

    override fun onTick() {
        val gv = gameView ?: return
        val debugWidget = gv.debugWidget
        val debugEntity = debugFamily.firstOrNull() ?: return
        val gameStateEntity = gameStateFamily.firstOrNull() ?: return

        val debugCmp = debugEntity[DebugComponent]
        val gameStateCmp = gameStateEntity[GameStateComponent]

        // --- 1. UI -> Game State ---

        // game
        debugCmp.debugTimeScale = debugWidget.gameSpeedSlider.value

        // sound
        gameStateCmp.musicVolume = debugWidget.musicVolumeSlider.value
        gameStateCmp.ambienceVolume = debugWidget.ambienceVolumeSlider.value
        gameStateCmp.sfxVolume = debugWidget.sfxVolumeSlider.value

        // physics
        debugCmp.drawPhysicBodies = debugWidget.physicBodiesDebugCheckBox.isChecked
        debugCmp.drawVelocities = debugWidget.velocityDebugCheckBox.isChecked
        debugCmp.playerDebugEnabled = debugWidget.playerDebugCheckBox.isChecked
        debugCmp.enemyDebugEnabled = debugWidget.enemyDebugCheckBox.isChecked
        debugCmp.attackDebugEnabled = debugWidget.attackDebugCheckBox.isChecked
        debugCmp.cameraDebugEnabled = debugWidget.cameraDebugCheckBox.isChecked

        // light
        debugCmp.directionalLightIntensity = debugWidget.directionalLightIntensitySlider.value
        debugCmp.box2dLightStrength = debugWidget.box2dLightStrengthSlider.value
        debugCmp.shaderIntensity = debugWidget.shaderIntensitySlider.value
        debugCmp.normalInfluence = debugWidget.normalInfluenceSlider.value
        debugCmp.specularIntensity = debugWidget.specularIntensitySlider.value
        debugCmp.sunElevation = debugWidget.sunElevationSlider.value
        debugCmp.useDiffuseLight = debugWidget.diffuseLightCheckBox.isChecked

        // --- 2. Game State -> UI ---

        // game
        val speedPercent = (debugCmp.debugTimeScale * 100).roundToInt()
        debugWidget.gameSpeedLabel.setText("Game speed: $speedPercent%")
        debugWidget.gameSpeedSlider.value = debugCmp.debugTimeScale

        // sound
        val musicPercent = (gameStateCmp.musicVolume * 100).roundToInt()
        val ambiencePercent = (gameStateCmp.ambienceVolume * 100).roundToInt()
        val sfxPercent = (gameStateCmp.sfxVolume * 100).roundToInt()
        debugWidget.musicVolumeLabel.setText("Music: $musicPercent%")
        debugWidget.ambienceVolumeLabel.setText("Ambience: $ambiencePercent%")
        debugWidget.sfxVolumeLabel.setText("Sfx: $sfxPercent%")

        // physics
        debugWidget.playerDebugCheckBox.isChecked = debugCmp.playerDebugEnabled
        debugWidget.enemyDebugCheckBox.isChecked = debugCmp.enemyDebugEnabled
        debugWidget.attackDebugCheckBox.isChecked = debugCmp.attackDebugEnabled
        debugWidget.cameraDebugCheckBox.isChecked = debugCmp.cameraDebugEnabled

        // light
        debugWidget.directionalLightIntensityLabel.setText("DirLight: %.2f".format(debugCmp.directionalLightIntensity))
        debugWidget.box2dLightStrengthLabel.setText("Box2D: %.2f".format(debugCmp.box2dLightStrength))
        debugWidget.shaderIntensityLabel.setText("Shader: %.2f".format(debugCmp.shaderIntensity))
        debugWidget.normalInfluenceLabel.setText("Normal Influence: %.2f".format(debugCmp.normalInfluence))
        debugWidget.specularIntensityLabel.setText("Specular Int.: %.2f".format(debugCmp.specularIntensity))
        debugWidget.sunElevationLabel.setText("Sun Elevation: %.0f".format(debugCmp.sunElevation))

        debugWidget.directionalLightIntensitySlider.value = debugCmp.directionalLightIntensity
        debugWidget.box2dLightStrengthSlider.value = debugCmp.box2dLightStrength
        debugWidget.shaderIntensitySlider.value = debugCmp.shaderIntensity
        debugWidget.normalInfluenceSlider.value = debugCmp.normalInfluence
        debugWidget.specularIntensitySlider.value = debugCmp.specularIntensity
        debugWidget.sunElevationSlider.value = debugCmp.sunElevation
        debugWidget.diffuseLightCheckBox.isChecked = debugCmp.useDiffuseLight

        DebugPropsManager.flush()
        debugWidget.setDebugProperties(debugRenderingService.renderToDebugProperties)
    }
}
