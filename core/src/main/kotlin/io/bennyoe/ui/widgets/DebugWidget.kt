package io.bennyoe.ui.widgets

import com.badlogic.gdx.graphics.profiling.GLProfiler
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Slider
import com.badlogic.gdx.scenes.scene2d.ui.Table
import io.bennyoe.ui.Drawables
import io.bennyoe.ui.widgets.debug.DrawCallsCounterWidget
import io.bennyoe.ui.widgets.debug.FpsCounterWidget
import io.bennyoe.ui.widgets.debug.FpsMillis
import ktx.actors.alpha
import ktx.scene2d.KTable
import ktx.style.get

class DebugWidget(
    skin: Skin,
    profiler: GLProfiler,
) : Table(skin),
    KTable {
    // metrics
    private val fpsCounter = FpsCounterWidget(skin["debug"])
    private val fpsMillis = FpsMillis(skin["debug"])
    private val drawCallsCounter = DrawCallsCounterWidget(skin["debug"], profiler)

    // game
    val gameSpeedSlider = Slider(0f, 2f, 0.1f, false, skin)
    val gameSpeedLabel: Label

    // sound
    val musicVolumeSlider = Slider(0f, 1f, 0.1f, false, skin)
    val musicVolumeLabel: Label
    val ambienceVolumeSlider = Slider(0f, 1f, 0.1f, false, skin)
    val ambienceVolumeLabel: Label
    val sfxVolumeSlider = Slider(0f, 1f, 0.1f, false, skin)
    val sfxVolumeLabel: Label

    // physics
    val playerDebugCheckBox: CheckBox
    val physicBodiesDebugCheckBox: CheckBox
    val velocityDebugCheckBox: CheckBox
    val enemyDebugCheckBox: CheckBox
    val attackDebugCheckBox: CheckBox
    val cameraDebugCheckBox: CheckBox

    // light
    val directionalLightIntensitySlider: Slider
    val directionalLightIntensityLabel: Label
    val box2dLightStrengthSlider: Slider
    val box2dLightStrengthLabel: Label
    val shaderAmbientSlider: Slider
    val shaderAmbientLabel: Label
    val shaderIntensitySlider: Slider
    val shaderIntensityLabel: Label
    val diffuseLightCheckBox: CheckBox
    val normalInfluenceSlider: Slider
    val normalInfluenceLabel: Label
    val specularIntensitySlider: Slider
    val specularIntensityLabel: Label
    val sunElevationSlider: Slider
    val sunElevationLabel: Label

    // dynamic properties (stats)
    private val debugPropsTable = Table(skin).left()
    private var lastKeys: List<String> = emptyList()

    init {
        gameSpeedSlider.value = 1f
        musicVolumeSlider.value = .6f
        ambienceVolumeSlider.value = .4f
        sfxVolumeSlider.value = .8f

        directionalLightIntensitySlider = Slider(0f, 1f, 0.01f, false, skin)
        directionalLightIntensitySlider.value = 1f
        box2dLightStrengthSlider = Slider(0f, 4f, 0.01f, false, skin)
        box2dLightStrengthSlider.value = 1.9f
        shaderAmbientSlider = Slider(0f, 10f, 0.1f, false, skin)
        shaderAmbientSlider.value = 2.2f
        shaderIntensitySlider = Slider(0f, 10f, 0.1f, false, skin)
        shaderIntensitySlider.value = 1.2f
        normalInfluenceSlider = Slider(0f, 1f, 0.01f, false, skin)
        normalInfluenceSlider.value = 1f
        specularIntensitySlider = Slider(0f, 10f, 0.01f, false, skin)
        specularIntensitySlider.value = 0.7f
        sunElevationSlider = Slider(0f, 90f, .1f, false, skin)
        sunElevationSlider.value = 60f

        background = skin.getDrawable(Drawables.DEBUG_FRAME.atlasKey)
        alpha = 0.8f
        pad(15f)

        defaults().left()
        left().top()

        add(fpsCounter).row()
        add(fpsMillis).row()
        add(drawCallsCounter).row()

        gameSpeedLabel = Label("Game speed: %03f%%".format(gameSpeedSlider.value), skin, "debug")
        gameSpeedLabel.pack()
        val reservedWidth = gameSpeedLabel.prefWidth

        musicVolumeLabel = Label("Music: %03f%%".format(musicVolumeSlider.value), skin, "debug")
        ambienceVolumeLabel = Label("Ambience: %03f%%".format(ambienceVolumeSlider.value), skin, "debug")
        sfxVolumeLabel = Label("SoundEffects: %03f%%".format(sfxVolumeSlider.value), skin, "debug")
        directionalLightIntensityLabel = Label("DirLight: ${directionalLightIntensitySlider.value}", skin, "debug")
        box2dLightStrengthLabel = Label("Box2D: ${box2dLightStrengthSlider.value}", skin, "debug")
        shaderAmbientLabel = Label("ShaderAmbient: ${shaderAmbientSlider.value}", skin, "debug")
        shaderIntensityLabel = Label("Shader: ${shaderIntensitySlider.value}", skin, "debug")

        normalInfluenceLabel = Label("Normal Influence: ${normalInfluenceSlider.value}", skin, "debug")
        specularIntensityLabel = Label("Specular Intensity: ${specularIntensitySlider.value}", skin, "debug")
        sunElevationLabel = Label("Sun Elevation: ${sunElevationSlider.value}", skin, "debug")

        val sliderTable = Table().left().padTop(10f)
        sliderTable.add(gameSpeedLabel).padRight(5f).width(reservedWidth)
        sliderTable.add(gameSpeedSlider).right().row()

        sliderTable
            .add(musicVolumeLabel)
            .padRight(5f)
            .padTop(10f)
            .width(reservedWidth)
        sliderTable
            .add(musicVolumeSlider)
            .right()
            .padTop(10f)
            .row()
        sliderTable.add(ambienceVolumeLabel).padRight(5f).width(reservedWidth)
        sliderTable.add(ambienceVolumeSlider).right().row()
        sliderTable.add(sfxVolumeLabel).padRight(5f).width(reservedWidth)
        sliderTable.add(sfxVolumeSlider).right().row()

        diffuseLightCheckBox = CheckBox("Diffuse Light", skin)
        diffuseLightCheckBox.isChecked = true
        sliderTable
            .add(directionalLightIntensityLabel)
            .padRight(5f)
            .padTop(15f)
            .width(reservedWidth)
        sliderTable
            .add(directionalLightIntensitySlider)
            .right()
            .padTop(16f)
            .row()
        sliderTable.add(box2dLightStrengthLabel).padRight(5f).width(reservedWidth)
        sliderTable.add(box2dLightStrengthSlider).right().row()
        sliderTable.add(shaderAmbientLabel).padRight(5f).width(reservedWidth)
        sliderTable.add(shaderAmbientSlider).right().row()
        sliderTable.add(shaderIntensityLabel).padRight(5f).width(reservedWidth)
        sliderTable.add(shaderIntensitySlider).right().row()

        sliderTable.add(normalInfluenceLabel).padRight(5f).width(reservedWidth)
        sliderTable.add(normalInfluenceSlider).right().row()
        sliderTable.add(specularIntensityLabel).padRight(5f).width(reservedWidth)
        sliderTable.add(specularIntensitySlider).right().row()
        sliderTable.add(sunElevationLabel).padRight(5f).width(reservedWidth)
        sliderTable.add(sunElevationSlider).right().row()

        sliderTable
            .add(diffuseLightCheckBox)
            .padTop(3f)
            .padBottom(10f)
            .left()
            .row()
        add(sliderTable).padBottom(10f).row()

        physicBodiesDebugCheckBox = CheckBox("PhysicBodies", skin)
        physicBodiesDebugCheckBox.isChecked = true
        velocityDebugCheckBox = CheckBox("Velocity (need Bodies)", skin)
        playerDebugCheckBox = CheckBox("PlayerDebug", skin)
        enemyDebugCheckBox = CheckBox("EnemyDebug", skin)
        attackDebugCheckBox = CheckBox("AttackDebug", skin)
        cameraDebugCheckBox = CheckBox("CameraDebug", skin)
        add(physicBodiesDebugCheckBox).padTop(3f).row()
        add(velocityDebugCheckBox).padTop(3f).row()
        add(playerDebugCheckBox).padTop(3f).row()
        add(enemyDebugCheckBox).padTop(3f).row()
        add(attackDebugCheckBox).padTop(3f).row()
        add(cameraDebugCheckBox).padTop(3f).row()

        add(debugPropsTable).growX().padTop(6f).row()
    }

    fun setDebugProperties(props: Map<String, Any>) {
        val keys = props.keys.sorted()

        if (keys != lastKeys) {
            debugPropsTable.clearChildren()
            for (k in keys) {
                val label = Label("", skin, "debug")
                debugPropsTable.add(label).left().row()
            }
            lastKeys = keys
        }

        for (k in keys) {
            val v = props[k] ?: continue
            val label = (debugPropsTable.children.find { it is Label && it.name == k } as? Label) ?: continue
            if (v is Number) {
                label.setText("$k ${formatNumber(v)}")
            } else {
                label.setText("$k $v")
            }
        }
        debugPropsTable.invalidateHierarchy()
    }

    private fun formatNumber(n: Number): String =
        when (n) {
            is Float, is Double -> "%.3f".format(n.toDouble())
            else -> n.toString()
        }
}
