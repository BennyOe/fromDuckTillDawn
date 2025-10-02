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
    private val fpsCounter = FpsCounterWidget(skin["debug"])
    private val fpsMillis = FpsMillis(skin["debug"])
    private val drawCallsCounter = DrawCallsCounterWidget(skin["debug"], profiler)
    val gameSpeedSlider = Slider(0f, 2f, 0.1f, false, skin)
    val gameSpeedLabel: Label

    val musicVolumeSlider = Slider(0f, 1f, 0.1f, false, skin)
    val musicVolumeLabel: Label

    val ambienceVolumeSlider = Slider(0f, 1f, 0.1f, false, skin)
    val ambienceVolumeLabel: Label

    val sfxVolumeSlider = Slider(0f, 1f, 0.1f, false, skin)
    val sfxVolumeLabel: Label

    val playerDebugCheckBox: CheckBox
    val physicBodiesDebugCheckBox: CheckBox
    val velocityDebugCheckBox: CheckBox
    val enemyDebugCheckBox: CheckBox
    val attackDebugCheckBox: CheckBox
    val cameraDebugCheckBox: CheckBox

    private val debugPropsTable = Table(skin).left()
    private val debugPropLabels = mutableMapOf<String, Label>()
    private var lastKeys: List<String> = emptyList()

    init {
        gameSpeedSlider.value = 1f
        musicVolumeSlider.value = .6f
        ambienceVolumeSlider.value = .4f
        sfxVolumeSlider.value = .8f

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

        val sliderTable = Table().left().padTop(10f)
        sliderTable.add(gameSpeedLabel).padRight(5f).width(reservedWidth)
        sliderTable.add(gameSpeedSlider).right().row()

        sliderTable
            .add(musicVolumeLabel)
            .padRight(5f)
            .padTop(10f)
            .width(reservedWidth)
        sliderTable.add(musicVolumeSlider).right().row()
        sliderTable.add(ambienceVolumeLabel).padRight(5f).width(reservedWidth)
        sliderTable.add(ambienceVolumeSlider).right().row()
        sliderTable.add(sfxVolumeLabel).padRight(5f).width(reservedWidth)
        sliderTable.add(sfxVolumeSlider).right()
        add(sliderTable).padBottom(10f).row()

        physicBodiesDebugCheckBox = CheckBox("PhysicBodies", skin)
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
        add(cameraDebugCheckBox).padTop(3f).padBottom(10f).row()

        add(debugPropsTable).growX().padTop(6f).row()
    }

    fun setDebugProperties(props: Map<String, Any>) {
        val keys = props.keys.sorted()

        if (keys != lastKeys) {
            debugPropsTable.clearChildren()
            debugPropLabels.clear()
            for (k in keys) {
                val label = Label("", skin, "debug")
                debugPropLabels[k] = label
                debugPropsTable.add(label).left().row()
            }
            lastKeys = keys
        }

        for (k in keys) {
            val v = props[k] ?: continue
            if (v is Number) {
                debugPropLabels[k]?.setText("$k ${formatNumber(v)}")
            } else {
                debugPropLabels[k]?.setText("$k $v")
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
