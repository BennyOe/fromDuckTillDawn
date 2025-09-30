package io.bennyoe.ui

import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Align
import io.bennyoe.ui.widgets.CharacterInfo
import io.bennyoe.ui.widgets.characterInfo
import ktx.scene2d.KTable
import ktx.scene2d.KWidget
import ktx.scene2d.Scene2DSkin
import ktx.scene2d.Scene2dDsl
import ktx.scene2d.actor

class GameView(
    skin: Skin,
) : Table(skin),
    KTable {
    private var characterInfo: CharacterInfo

    init {
        setFillParent(true)
        align(Align.bottom)
        characterInfo =
            characterInfo(skin) {
                // here you can place and align the widgets
//            it.row()
            }
    }

    fun playerLife(value: Float) {
        characterInfo.lifeBar.value = value
    }
}

/**
 * Adds DSL that creates [GameView].
 * @param skin defines style of the widget.
 * @param init customization code.
 */
@Scene2dDsl
inline fun <S> KWidget<S>.gameView(
    // This ensures a Skin is supplied by default:
    skin: Skin = Scene2DSkin.defaultSkin,
    // This supplies a default style name, making it optional:
    init: GameView.(S) -> Unit = {},
): GameView = actor(GameView(skin), init)
