package io.bennyoe.ui.widgets

import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import ktx.scene2d.KTable
import ktx.scene2d.KWidget
import ktx.scene2d.Scene2DSkin
import ktx.scene2d.Scene2dDsl
import ktx.scene2d.actor

class CharacterInfo(
    skin: Skin,
) : Table(skin),
    KTable {
    val lifeBar: ProgressBar = ProgressBar(0f, 1f, 0.1f, false, skin, "life-bar")
    val airBar: ProgressBar = ProgressBar(0f, 1f, 0.1f, false, skin, "air-bar")

    init {
        add(lifeBar).center()
        row()
        add(airBar).center().padTop(10f).padBottom(10f)
        lifeBar.value = 0.8f
        lifeBar.setAnimateDuration(0.5f)
        airBar.value = 0.8f
        airBar.setAnimateDuration(0.5f)
    }
}

/**
 * Adds DSL that creates [CharacterInfo].
 * @param skin defines style of the widget.
 * @param init customization code.
 */
@Scene2dDsl
inline fun <S> KWidget<S>.characterInfo(
    // This ensures a Skin is supplied by default:
    skin: Skin = Scene2DSkin.defaultSkin,
    // This supplies a default style name, making it optional:
    init: CharacterInfo.(S) -> Unit = {},
): CharacterInfo = actor(CharacterInfo(skin), init)
