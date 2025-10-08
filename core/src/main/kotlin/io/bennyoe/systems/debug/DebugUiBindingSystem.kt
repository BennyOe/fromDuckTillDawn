package io.bennyoe.systems.debug

import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.quillraven.fleks.IntervalSystem
import com.github.quillraven.fleks.World.Companion.inject
import io.bennyoe.components.GameStateComponent
import io.bennyoe.components.debug.DebugComponent
import io.bennyoe.systems.light.AmbientLightSystem
import io.bennyoe.systems.light.LightingParameters
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
    private val ambientLightSystem: AmbientLightSystem by lazy { world.system<AmbientLightSystem>() }

    private var lastKnownParams: LightingParameters? = null

    override fun onTick() {
        val gameView = gameView ?: return
        val debugWidget = gameView.debugWidget
        val debugEntity = debugFamily.firstOrNull() ?: return
        val gameStateEntity = gameStateFamily.firstOrNull() ?: return

        val debugCmp = debugEntity[DebugComponent]
        val gameStateCmp = gameStateEntity[GameStateComponent]

        // --- 1. Synchronize Lighting Parameters (Two-Way) ---
        val programmaticParams = ambientLightSystem.currentParams

        // Initialize lastKnownParams on the first run
        if (lastKnownParams == null) {
            lastKnownParams = programmaticParams.copy()
        }

        // Check if the game logic has changed the parameters (e.g., zone change)
        if (programmaticParams != lastKnownParams) {
            // Game state has changed -> Update the UI to reflect it
            debugWidget.directionalLightIntensitySlider.value = programmaticParams.directionalLightIntensity
            debugWidget.box2dLightStrengthSlider.value = programmaticParams.box2dLightStrength
            debugWidget.shaderIntensitySlider.value = programmaticParams.shaderIntensity
            debugWidget.shaderAmbientSlider.value = programmaticParams.shaderAmbientStrength
            debugWidget.normalInfluenceSlider.value = programmaticParams.normalInfluence
            debugWidget.specularIntensitySlider.value = programmaticParams.specularIntensity
            debugWidget.sunElevationSlider.value = programmaticParams.sunElevation
            debugWidget.diffuseLightCheckBox.isChecked = programmaticParams.useDiffuseLight
        } else {
            // No programmatic change -> UI is the source of truth, update the game state
            ambientLightSystem.currentParams.apply {
                directionalLightIntensity = debugWidget.directionalLightIntensitySlider.value
                box2dLightStrength = debugWidget.box2dLightStrengthSlider.value
                shaderIntensity = debugWidget.shaderIntensitySlider.value
                shaderAmbientStrength = debugWidget.shaderAmbientSlider.value
                normalInfluence = debugWidget.normalInfluenceSlider.value
                specularIntensity = debugWidget.specularIntensitySlider.value
                sunElevation = debugWidget.sunElevationSlider.value
                useDiffuseLight = debugWidget.diffuseLightCheckBox.isChecked
            }
        }

        // Update our copy for the next frame's comparison
        lastKnownParams = ambientLightSystem.currentParams.copy()

        // --- 2. Bind other Debug settings (UI -> Game) ---
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

        // --- 3. Update UI Labels (Game -> UI) ---
        val speedPercent = (debugCmp.debugTimeScale * 100).roundToInt()
        debugWidget.gameSpeedLabel.setText("Game speed: $speedPercent%")

        val musicPercent = (gameStateCmp.musicVolume * 100).roundToInt()
        val ambiencePercent = (gameStateCmp.ambienceVolume * 100).roundToInt()
        val sfxPercent = (gameStateCmp.sfxVolume * 100).roundToInt()
        debugWidget.musicVolumeLabel.setText("Music: $musicPercent%")
        debugWidget.ambienceVolumeLabel.setText("Ambience: $ambiencePercent%")
        debugWidget.sfxVolumeLabel.setText("Sfx: $sfxPercent%")

        debugWidget.playerDebugCheckBox.isChecked = debugCmp.playerDebugEnabled
        debugWidget.enemyDebugCheckBox.isChecked = debugCmp.enemyDebugEnabled
        debugWidget.attackDebugCheckBox.isChecked = debugCmp.attackDebugEnabled
        debugWidget.cameraDebugCheckBox.isChecked = debugCmp.cameraDebugEnabled

        // Read directly from the sliders for the labels to give immediate feedback
        debugWidget.directionalLightIntensityLabel.setText("DirLight: %.2f".format(debugWidget.directionalLightIntensitySlider.value))
        debugWidget.box2dLightStrengthLabel.setText("Box2D: %.2f".format(debugWidget.box2dLightStrengthSlider.value))
        debugWidget.shaderIntensityLabel.setText("Shader: %.2f".format(debugWidget.shaderIntensitySlider.value))
        debugWidget.shaderAmbientLabel.setText("ShaderAmbient: %.2f".format(debugWidget.shaderAmbientSlider.value))
        debugWidget.normalInfluenceLabel.setText("Normal Influence: %.2f".format(debugWidget.normalInfluenceSlider.value))
        debugWidget.specularIntensityLabel.setText("Specular Int.: %.2f".format(debugWidget.specularIntensitySlider.value))
        debugWidget.sunElevationLabel.setText("Sun Elevation: %.0f".format(debugWidget.sunElevationSlider.value))

        DebugPropsManager.flush()
        debugWidget.setDebugProperties(debugRenderingService.renderToDebugProperties)
    }
}
